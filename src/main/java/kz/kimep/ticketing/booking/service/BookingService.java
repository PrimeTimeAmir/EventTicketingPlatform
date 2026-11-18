package kz.kimep.ticketing.booking.service;

import kz.kimep.ticketing.support.audit.service.AuditService;
import kz.kimep.ticketing.booking.api.dto.BookingRequest;
import kz.kimep.ticketing.booking.api.dto.BookingResponse;
import kz.kimep.ticketing.booking.domain.Booking;
import kz.kimep.ticketing.booking.domain.BookingStatus;
import kz.kimep.ticketing.booking.domain.HoldStatus;
import kz.kimep.ticketing.booking.domain.SeatHold;
import kz.kimep.ticketing.booking.repo.BookingRepository;
import kz.kimep.ticketing.booking.repo.SeatHoldRepository;
import kz.kimep.ticketing.catalog.domain.Event;
import kz.kimep.ticketing.catalog.domain.EventSectionPrice;
import kz.kimep.ticketing.catalog.domain.EventStatus;
import kz.kimep.ticketing.catalog.domain.Seat;
import kz.kimep.ticketing.catalog.repo.EventRepository;
import kz.kimep.ticketing.catalog.repo.EventSectionPriceRepository;
import kz.kimep.ticketing.catalog.repo.SeatRepository;
import kz.kimep.ticketing.identity.domain.User;
import kz.kimep.ticketing.identity.repo.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final SeatHoldRepository seatHoldRepository;
    private final EventRepository eventRepository;
    private final SeatRepository seatRepository;
    private final EventSectionPriceRepository priceRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public BookingService(
            BookingRepository bookingRepository,
            SeatHoldRepository seatHoldRepository,
            EventRepository eventRepository,
            SeatRepository seatRepository,
            EventSectionPriceRepository priceRepository,
            UserRepository userRepository,
            AuditService auditService
    ) {
        this.bookingRepository = bookingRepository;
        this.seatHoldRepository = seatHoldRepository;
        this.eventRepository = eventRepository;
        this.seatRepository = seatRepository;
        this.priceRepository = priceRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional
    public BookingResponse createBooking(
            BookingRequest request,
            String userEmail
    ) {

        // 1. Find user
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));

        // 2. Lock hold
        SeatHold hold = seatHoldRepository
                .findByHoldReferenceForUpdate(request.holdReference())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Seat hold not found"
                ));

        // 3. Check ownership
        if (!hold.getUserId().equals(user.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "This seat hold belongs to another user"
            );
        }

        // 4. Check hold status
        OffsetDateTime now = OffsetDateTime.now();

        if (hold.getStatus() != HoldStatus.ACTIVE
                || !hold.getExpiresAt().isAfter(now)) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Seat hold has expired or is no longer active"
            );
        }

        // 5. Check duplicate booking
        if (bookingRepository.existsBySeatHold_Id(hold.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A booking already exists for this seat hold"
            );
        }

        // 6. Find event
        Event event = eventRepository.findById(hold.getEventId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Event not found"
                ));

        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Event is no longer available"
            );
        }

        // 7. Check sales period
        if (event.getSalesOpenAt() == null
                || event.getSalesCloseAt() == null
                || now.isBefore(event.getSalesOpenAt())
                || !now.isBefore(event.getSalesCloseAt())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ticket sales are closed"
            );
        }

        // 8. Load and lock seats
        List<Seat> seats =
                seatRepository.findAllByIdInForUpdate(hold.getSeatIds());

        if (seats.size() != hold.getSeatIds().size()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "One or more reserved seats no longer exist"
            );
        }

        // 9. Verify venue
        for (Seat seat : seats) {

            Long venueId = seat.getSection().getVenue().getId();

            if (!venueId.equals(event.getVenue().getId())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "A reserved seat does not belong to the event venue"
                );
            }
        }

        // 10. Load event prices
        List<EventSectionPrice> eventPrices =
                priceRepository.findAllByEvent_Id(event.getId());

        Map<Long, BigDecimal> pricesBySection =
                eventPrices.stream().collect(Collectors.toMap(
                        price -> price.getSection().getId(),
                        EventSectionPrice::getPrice
                ));

        // 11. Calculate subtotal
        BigDecimal subtotal = BigDecimal.ZERO;

        for (Seat seat : seats) {

            Long sectionId = seat.getSection().getId();

            BigDecimal price = pricesBySection.get(sectionId);

            if (price == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Price not configured for a reserved section"
                );
            }

            subtotal = subtotal.add(price);
        }

        // 12. Calculate fees and total
        BigDecimal fees = BigDecimal.ZERO;

        BigDecimal total = subtotal.add(fees);

        // 13. Create booking
        Booking booking = new Booking(
                UUID.randomUUID().toString(),
                hold,
                user.getId(),
                event.getId(),
                subtotal,
                fees,
                total
        );

        Booking savedBooking = bookingRepository.save(booking);

        auditService.record(
                user.getId(),
                "BOOKING_CREATED",
                "BOOKING",
                savedBooking.getId().toString()
        );

        // 14. Return response
        return BookingResponse.from(savedBooking);
    }
}