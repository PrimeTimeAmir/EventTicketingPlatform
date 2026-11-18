package kz.kimep.ticketing.booking.service;

import kz.kimep.ticketing.booking.api.dto.SeatHoldRequest;
import kz.kimep.ticketing.booking.api.dto.SeatHoldResponse;
import kz.kimep.ticketing.booking.domain.HoldStatus;
import kz.kimep.ticketing.booking.domain.SeatHold;
import kz.kimep.ticketing.booking.repo.SeatHoldRepository;
import kz.kimep.ticketing.booking.repo.TicketRepository;
import kz.kimep.ticketing.catalog.domain.Event;
import kz.kimep.ticketing.catalog.domain.EventSectionPrice;
import kz.kimep.ticketing.catalog.domain.EventStatus;
import kz.kimep.ticketing.catalog.domain.Seat;
import kz.kimep.ticketing.catalog.repo.EventRepository;
import kz.kimep.ticketing.catalog.repo.EventSectionPriceRepository;
import kz.kimep.ticketing.catalog.repo.SeatRepository;
import kz.kimep.ticketing.identity.domain.User;
import kz.kimep.ticketing.identity.repo.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SeatHoldService {

    private final SeatHoldRepository seatHoldRepository;
    private final TicketRepository ticketRepository;
    private final EventRepository eventRepository;
    private final SeatRepository seatRepository;
    private final EventSectionPriceRepository priceRepository;
    private final UserRepository userRepository;

    @Value("${app.booking.hold-ttl-minutes:10}")
    private long holdTtlMinutes;

    public SeatHoldService(
            SeatHoldRepository seatHoldRepository,
            TicketRepository ticketRepository,
            EventRepository eventRepository,
            SeatRepository seatRepository,
            EventSectionPriceRepository priceRepository,
            UserRepository userRepository
    ) {
        this.seatHoldRepository = seatHoldRepository;
        this.ticketRepository = ticketRepository;
        this.eventRepository = eventRepository;
        this.seatRepository = seatRepository;
        this.priceRepository = priceRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public SeatHoldResponse createHold(
            Long eventId,
            SeatHoldRequest request,
            String userEmail
    ) {

        // 1. Validate requested seat IDs
        Set<Long> requestedSeatIds = new HashSet<>(request.seatIds());

        if (requestedSeatIds.size() != request.seatIds().size()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Duplicate seat IDs are not allowed"
            );
        }

        // 2. Find event
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Event not found"
                ));

        // 3. Check event status
        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Event is not available for booking"
            );
        }

        // 4. Check sales period
        OffsetDateTime now = OffsetDateTime.now();

        if (event.getSalesOpenAt() == null
                || event.getSalesCloseAt() == null
                || now.isBefore(event.getSalesOpenAt())
                || !now.isBefore(event.getSalesCloseAt())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ticket sales are currently closed"
            );
        }

        // 5. Find user
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));

        // 6. Lock requested seats in the database
        List<Seat> seats =
                seatRepository.findAllByIdInForUpdate(requestedSeatIds);

        if (seats.size() != requestedSeatIds.size()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "One or more seats were not found"
            );
        }

        // 7. Check that all seats belong to the event venue
        for (Seat seat : seats) {

            Long seatVenueId = seat.getSection().getVenue().getId();

            if (!seatVenueId.equals(event.getVenue().getId())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "A selected seat does not belong to the event venue"
                );
            }
        }

        // 8. Check existing tickets
        boolean alreadyTicketed =
                !ticketRepository.findAllByEventIdAndSeatIdIn(
                        eventId,
                        requestedSeatIds
                ).isEmpty();

        if (alreadyTicketed) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "One or more selected seats have already been sold"
            );
        }

        // 9. Check active holds
        List<SeatHold> activeHolds =
                seatHoldRepository.findAllByEventIdAndStatusAndExpiresAtAfter(
                        eventId,
                        HoldStatus.ACTIVE,
                        now
                );

        for (SeatHold activeHold : activeHolds) {

            Set<Long> heldSeatIds = activeHold.getSeatIds();

            boolean overlap = requestedSeatIds.stream()
                    .anyMatch(heldSeatIds::contains);

            if (overlap) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "One or more selected seats are currently reserved"
                );
            }
        }

        // 10. Load prices for the event
        List<EventSectionPrice> eventPrices =
                priceRepository.findAllByEvent_Id(eventId);

        Map<Long, BigDecimal> pricesBySection =
                eventPrices.stream().collect(Collectors.toMap(
                        price -> price.getSection().getId(),
                        EventSectionPrice::getPrice,
                        (first, second) -> first
                ));

        // 11. Calculate subtotal
        BigDecimal subtotal = BigDecimal.ZERO;

        for (Seat seat : seats) {

            Long sectionId = seat.getSection().getId();

            BigDecimal seatPrice = pricesBySection.get(sectionId);

            if (seatPrice == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "No price configured for one of the selected sections"
                );
            }

            subtotal = subtotal.add(seatPrice);
        }

        // 12. Create temporary hold
        OffsetDateTime expiresAt = now.plusMinutes(holdTtlMinutes);

        SeatHold hold = new SeatHold(
                UUID.randomUUID().toString(),
                eventId,
                user.getId(),
                requestedSeatIds,
                expiresAt
        );

        SeatHold savedHold = seatHoldRepository.save(hold);

        // 13. Return response
        return new SeatHoldResponse(
                savedHold.getHoldReference(),
                eventId,
                savedHold.getSeatIds(),
                savedHold.getExpiresAt(),
                savedHold.getStatus(),
                subtotal,
                "KZT"
        );
    }
}