package kz.kimep.ticketing.payment.service;

import kz.kimep.ticketing.support.audit.service.AuditService;
import kz.kimep.ticketing.messaging.service.OutboxService;
import kz.kimep.ticketing.booking.domain.*;
import kz.kimep.ticketing.booking.repo.*;
import kz.kimep.ticketing.catalog.domain.*;
import kz.kimep.ticketing.catalog.repo.*;
import kz.kimep.ticketing.identity.domain.User;
import kz.kimep.ticketing.identity.repo.UserRepository;
import kz.kimep.ticketing.payment.api.dto.*;
import kz.kimep.ticketing.payment.domain.*;
import kz.kimep.ticketing.payment.repo.PaymentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final SeatHoldRepository seatHoldRepository;
    private final TicketRepository ticketRepository;
    private final EventRepository eventRepository;
    private final SeatRepository seatRepository;
    private final EventSectionPriceRepository priceRepository;
    private final UserRepository userRepository;
    private final OutboxService outboxService;
    private final AuditService auditService;

    public PaymentService(
            PaymentRepository paymentRepository,
            BookingRepository bookingRepository,
            SeatHoldRepository seatHoldRepository,
            TicketRepository ticketRepository,
            EventRepository eventRepository,
            SeatRepository seatRepository,
            EventSectionPriceRepository priceRepository,
            UserRepository userRepository,
            OutboxService outboxService,
            AuditService auditService
    ) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.seatHoldRepository = seatHoldRepository;
        this.ticketRepository = ticketRepository;
        this.eventRepository = eventRepository;
        this.seatRepository = seatRepository;
        this.priceRepository = priceRepository;
        this.userRepository = userRepository;
        this.outboxService = outboxService;
        this.auditService = auditService;
    }

    // CREATE PAYMENT

    @Transactional
    public PaymentResponse createPayment(
            PaymentRequest request,
            String userEmail
    ) {

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));

        // Lock booking
        Booking booking = bookingRepository
                .findByBookingReferenceForUpdate(
                        request.bookingReference()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Booking not found"
                ));

        // Check ownership
        if (!booking.getUserId().equals(user.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "This booking belongs to another user"
            );
        }

        // Check booking status
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Booking is not awaiting payment"
            );
        }

        // Lock seat hold
        SeatHold hold = seatHoldRepository
                .findByHoldReferenceForUpdate(
                        booking.getSeatHold().getHoldReference()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Seat hold not found"
                ));

        OffsetDateTime now = OffsetDateTime.now();

        if (hold.getStatus() != HoldStatus.ACTIVE
                || !hold.getExpiresAt().isAfter(now)) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Seat hold has expired"
            );
        }

        // Prevent duplicate payments
        if (paymentRepository.existsByBooking_Id(booking.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Payment already exists for this booking"
            );
        }

        // Create pending payment
        Payment payment = new Payment(
                "TXN-" + UUID.randomUUID(),
                booking,
                request.method(),
                booking.getTotal()
        );

        Payment savedPayment = paymentRepository.save(payment);

        auditService.record(
                user.getId(),
                "PAYMENT_CREATED",
                "PAYMENT",
                savedPayment.getTransactionId()
        );

        return PaymentResponse.from(savedPayment);
    }


    // SIMULATE PAYMENT RESULT

    @Transactional
    public PaymentSimulationResponse simulatePayment(
            String transactionId,
            PaymentSimulationRequest request,
            String userEmail
    ) {

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));

        // First read payment to get its booking reference
        Payment initialPayment = paymentRepository
                .findByTransactionId(transactionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Payment not found"
                ));

        // Lock booking first
        Booking booking = bookingRepository
                .findByBookingReferenceForUpdate(
                        initialPayment.getBooking().getBookingReference()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Booking not found"
                ));

        // Lock payment
        Payment payment = paymentRepository
                .findByTransactionIdForUpdate(transactionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Payment not found"
                ));

        // Check ownership
        if (!booking.getUserId().equals(user.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "This payment belongs to another user"
            );
        }

        if (payment.getStatus() != PaymentStatus.PENDING
                || booking.getStatus() != BookingStatus.PENDING) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Payment has already been processed"
            );
        }

        // Lock hold
        SeatHold hold = seatHoldRepository
                .findByHoldReferenceForUpdate(
                        booking.getSeatHold().getHoldReference()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Seat hold not found"
                ));

        OffsetDateTime now = OffsetDateTime.now();

        // Check hold validity
        if (hold.getStatus() != HoldStatus.ACTIVE
                || !hold.getExpiresAt().isAfter(now)) {

            failPayment(
                    payment,
                    booking,
                    hold,
                    "HOLD_EXPIRED",
                    now
            );

            return buildResult(payment, booking, List.of());
        }

        // Check event availability
        Event event = eventRepository.findById(booking.getEventId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Event not found"
                ));

        if (event.getStatus() != EventStatus.PUBLISHED) {

            failPayment(
                    payment,
                    booking,
                    hold,
                    "EVENT_UNAVAILABLE",
                    now
            );

            return buildResult(payment, booking, List.of());
        }

        // Simulated payment failure
        if (!Boolean.TRUE.equals(request.successful())) {

            String failureCode = request.failureCode();

            if (failureCode == null || failureCode.isBlank()) {
                failureCode = "SIMULATED_FAILURE";
            }

            failPayment(
                    payment,
                    booking,
                    hold,
                    failureCode,
                    now
            );

            return buildResult(payment, booking, List.of());
        }

        // Lock seats
        List<Seat> seats =
                seatRepository.findAllByIdInForUpdate(
                        hold.getSeatIds()
                );

        if (seats.size() != hold.getSeatIds().size()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "One or more reserved seats no longer exist"
            );
        }

        // Load event prices
        List<EventSectionPrice> eventPrices =
                priceRepository.findAllByEvent_Id(event.getId());

        Map<Long, BigDecimal> pricesBySection =
                eventPrices.stream().collect(Collectors.toMap(
                        price -> price.getSection().getId(),
                        EventSectionPrice::getPrice
                ));

        // Prevent duplicate tickets
        if (!ticketRepository.findAllByEventIdAndSeatIdIn(
                event.getId(),
                hold.getSeatIds()
        ).isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A ticket already exists for one of the reserved seats"
            );
        }

        // Create tickets
        List<Ticket> tickets = new ArrayList<>();

        for (Seat seat : seats) {

            BigDecimal seatPrice =
                    pricesBySection.get(seat.getSection().getId());

            if (seatPrice == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Price not configured for a reserved section"
                );
            }

            Ticket ticket = new Ticket(
                    "TKT-" + UUID.randomUUID(),
                    booking,
                    event.getId(),
                    seat.getId(),
                    seatPrice
            );

            tickets.add(ticket);
        }

        List<Ticket> savedTickets =
                ticketRepository.saveAll(tickets);

        // Update payment
        payment.setStatus(PaymentStatus.SUCCEEDED);
        payment.setPaidAt(now);

        // Confirm booking
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setConfirmedAt(now);


        // Consume hold
        hold.setStatus(HoldStatus.CONSUMED);

        // Prepare Outbox event payload
        List<Long> ticketIds = savedTickets.stream()
                .map(Ticket::getId)
                .toList();

        Map<String, Object> eventPayload = Map.of(
                "bookingId", booking.getId(),
                "bookingReference", booking.getBookingReference(),
                "transactionId", payment.getTransactionId(),
                "eventId", event.getId(),
                "ticketCount", savedTickets.size(),
                "ticketIds", ticketIds
        );

        // Save event in the same database transaction
        outboxService.saveEvent(
                "BOOKING",
                booking.getId().toString(),
                "TICKETS_ISSUED",
                eventPayload
        );

        auditService.record(
                user.getId(),
                "PAYMENT_SUCCEEDED",
                "PAYMENT",
                payment.getTransactionId()
        );

        auditService.record(
                user.getId(),
                "TICKETS_ISSUED",
                "BOOKING",
                booking.getId().toString()
        );

        return buildResult(payment, booking, savedTickets);
    }


    // HANDLE FAILED PAYMENT

    private void failPayment(
            Payment payment,
            Booking booking,
            SeatHold hold,
            String failureCode,
            OffsetDateTime now
    ) {

        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureCode(failureCode);

        booking.setStatus(BookingStatus.PAYMENT_FAILED);

        auditService.record(
                booking.getUserId(),
                "PAYMENT_FAILED",
                "PAYMENT",
                payment.getTransactionId()
        );

        if (hold.getStatus() == HoldStatus.ACTIVE) {

            if (hold.getExpiresAt().isAfter(now)) {
                hold.setStatus(HoldStatus.RELEASED);
            } else {
                hold.setStatus(HoldStatus.EXPIRED);
            }
        }
    }


    // BUILD RESPONSE

    private PaymentSimulationResponse buildResult(
            Payment payment,
            Booking booking,
            List<Ticket> tickets
    ) {

        List<TicketResponse> ticketResponses =
                tickets.stream()
                        .map(TicketResponse::from)
                        .toList();

        return new PaymentSimulationResponse(
                PaymentResponse.from(payment),
                booking.getStatus(),
                ticketResponses
        );
    }
}