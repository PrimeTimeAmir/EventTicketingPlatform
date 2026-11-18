package kz.kimep.ticketing.payment;

import kz.kimep.ticketing.messaging.service.OutboxService;
import kz.kimep.ticketing.support.audit.service.AuditService;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Map;
import kz.kimep.ticketing.booking.domain.*;
import kz.kimep.ticketing.booking.repo.*;
import kz.kimep.ticketing.catalog.domain.*;
import kz.kimep.ticketing.catalog.repo.*;
import kz.kimep.ticketing.identity.domain.User;
import kz.kimep.ticketing.identity.repo.UserRepository;
import kz.kimep.ticketing.payment.api.dto.*;
import kz.kimep.ticketing.payment.domain.*;
import kz.kimep.ticketing.payment.repo.PaymentRepository;
import kz.kimep.ticketing.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private AuditService auditService;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private SeatHoldRepository seatHoldRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private EventSectionPriceRepository priceRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private PaymentService paymentService;


    // TEST 1: Create pending payment

    @Test
    void shouldCreatePendingPayment() {

        OffsetDateTime now = OffsetDateTime.now();

        User user = mock(User.class);

        when(userRepository.findByEmail("customer@example.com"))
                .thenReturn(Optional.of(user));

        when(user.getId()).thenReturn(7L);

        SeatHold hold = new SeatHold(
                "hold-123",
                20L,
                7L,
                Set.of(1L),
                now.plusMinutes(10)
        );

        Booking booking = new Booking(
                "booking-123",
                hold,
                7L,
                20L,
                new BigDecimal("15000.00"),
                BigDecimal.ZERO,
                new BigDecimal("15000.00")
        );

        ReflectionTestUtils.setField(booking, "id", 123L);

        when(bookingRepository.findByBookingReferenceForUpdate(
                "booking-123"
        )).thenReturn(Optional.of(booking));

        when(seatHoldRepository.findByHoldReferenceForUpdate(
                "hold-123"
        )).thenReturn(Optional.of(hold));

        when(paymentRepository.existsByBooking_Id(123L))
                .thenReturn(false);

        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = paymentService.createPayment(
                new PaymentRequest("booking-123", PaymentMethod.CARD),
                "customer@example.com"
        );

        assertNotNull(response);
        assertNotNull(response.transactionId());

        assertEquals("booking-123", response.bookingReference());
        assertEquals(PaymentMethod.CARD, response.method());
        assertEquals(PaymentStatus.PENDING, response.status());
        assertEquals("KZT", response.currency());

        assertEquals(
                0,
                new BigDecimal("15000.00").compareTo(response.amount())
        );

        verify(paymentRepository).save(any(Payment.class));
    }


    // TEST 2: Successful payment and ticket issuing

    @Test
    void shouldConfirmBookingAndIssueTicketsAfterSuccessfulPayment() {

        OffsetDateTime now = OffsetDateTime.now();

        User user = mock(User.class);

        when(userRepository.findByEmail("customer@example.com"))
                .thenReturn(Optional.of(user));

        when(user.getId()).thenReturn(7L);

        SeatHold hold = new SeatHold(
                "hold-456",
                20L,
                7L,
                Set.of(1L, 2L),
                now.plusMinutes(10)
        );

        Booking booking = new Booking(
                "booking-456",
                hold,
                7L,
                20L,
                new BigDecimal("30000.00"),
                BigDecimal.ZERO,
                new BigDecimal("30000.00")
        );

        ReflectionTestUtils.setField(booking, "id", 456L);

        Payment payment = new Payment(
                "TXN-TEST-123",
                booking,
                PaymentMethod.CARD,
                new BigDecimal("30000.00")
        );

        when(paymentRepository.findByTransactionId("TXN-TEST-123"))
                .thenReturn(Optional.of(payment));

        when(paymentRepository.findByTransactionIdForUpdate("TXN-TEST-123"))
                .thenReturn(Optional.of(payment));

        when(bookingRepository.findByBookingReferenceForUpdate(
                "booking-456"
        )).thenReturn(Optional.of(booking));

        when(seatHoldRepository.findByHoldReferenceForUpdate(
                "hold-456"
        )).thenReturn(Optional.of(hold));

        Event event = mock(Event.class);

        when(eventRepository.findById(20L))
                .thenReturn(Optional.of(event));

        when(event.getId()).thenReturn(20L);
        when(event.getStatus()).thenReturn(EventStatus.PUBLISHED);

        Section section = mock(Section.class);

        when(section.getId()).thenReturn(30L);

        Seat seat1 = mock(Seat.class);
        Seat seat2 = mock(Seat.class);

        // Set seat IDs
        when(seat1.getId()).thenReturn(1L);
        when(seat2.getId()).thenReturn(2L);

        // Set sections
        when(seat1.getSection()).thenReturn(section);
        when(seat2.getSection()).thenReturn(section);

        when(seatRepository.findAllByIdInForUpdate(Set.of(1L, 2L)))
                .thenReturn(List.of(seat1, seat2));

        EventSectionPrice sectionPrice = mock(EventSectionPrice.class);

        when(sectionPrice.getSection()).thenReturn(section);
        when(sectionPrice.getPrice())
                .thenReturn(new BigDecimal("15000.00"));

        when(priceRepository.findAllByEvent_Id(20L))
                .thenReturn(List.of(sectionPrice));

        when(ticketRepository.findAllByEventIdAndSeatIdIn(
                20L,
                Set.of(1L, 2L)
        )).thenReturn(List.of());

        when(ticketRepository.saveAll(anyList()))
                .thenAnswer(invocation -> {
                    List<Ticket> savedTickets = invocation.getArgument(0);

                    ReflectionTestUtils.setField(
                            savedTickets.get(0), "id", 101L
                    );

                    ReflectionTestUtils.setField(
                            savedTickets.get(1), "id", 102L
                    );

                    return savedTickets;
                });

        PaymentSimulationRequest request =
                new PaymentSimulationRequest(true, null);

        PaymentSimulationResponse response =
                paymentService.simulatePayment(
                        "TXN-TEST-123",
                        request,
                        "customer@example.com"
                );

        assertNotNull(response);

        assertEquals(
                PaymentStatus.SUCCEEDED,
                response.payment().status()
        );

        assertEquals(
                BookingStatus.CONFIRMED,
                response.bookingStatus()
        );

        assertEquals(HoldStatus.CONSUMED, hold.getStatus());

        assertEquals(2, response.tickets().size());

        assertTrue(
                response.tickets().stream()
                        .allMatch(t -> t.status() == TicketStatus.ISSUED)
        );

        assertEquals(
                Set.of(1L, 2L),
                response.tickets().stream()
                        .map(TicketResponse::seatId)
                        .collect(java.util.stream.Collectors.toSet())
        );

        assertNotNull(response.payment().paidAt());

        verify(ticketRepository).saveAll(anyList());

        verify(outboxService).saveEvent(
                eq("BOOKING"),
                eq("456"),
                eq("TICKETS_ISSUED"),
                eq(Map.of(
                        "bookingId", 456L,
                        "bookingReference", "booking-456",
                        "transactionId", "TXN-TEST-123",
                        "eventId", 20L,
                        "ticketCount", 2,
                        "ticketIds", List.of(101L, 102L)
                ))
        );
    }


    // TEST 3: Failed payment and releasing the hold

    @Test
    void shouldFailPaymentAndReleaseHoldWithoutTickets() {

        OffsetDateTime now = OffsetDateTime.now();

        User user = mock(User.class);

        when(userRepository.findByEmail("customer@example.com"))
                .thenReturn(Optional.of(user));

        when(user.getId()).thenReturn(7L);

        SeatHold hold = new SeatHold(
                "hold-789",
                20L,
                7L,
                Set.of(3L),
                now.plusMinutes(10)
        );

        Booking booking = new Booking(
                "booking-789",
                hold,
                7L,
                20L,
                new BigDecimal("15000.00"),
                BigDecimal.ZERO,
                new BigDecimal("15000.00")
        );

        Payment payment = new Payment(
                "TXN-TEST-456",
                booking,
                PaymentMethod.CARD,
                new BigDecimal("15000.00")
        );

        when(paymentRepository.findByTransactionId("TXN-TEST-456"))
                .thenReturn(Optional.of(payment));

        when(paymentRepository.findByTransactionIdForUpdate("TXN-TEST-456"))
                .thenReturn(Optional.of(payment));

        when(bookingRepository.findByBookingReferenceForUpdate(
                "booking-789"
        )).thenReturn(Optional.of(booking));

        when(seatHoldRepository.findByHoldReferenceForUpdate(
                "hold-789"
        )).thenReturn(Optional.of(hold));

        Event event = mock(Event.class);

        when(eventRepository.findById(20L))
                .thenReturn(Optional.of(event));

        when(event.getStatus()).thenReturn(EventStatus.PUBLISHED);

        PaymentSimulationRequest request =
                new PaymentSimulationRequest(
                        false,
                        "CARD_DECLINED"
                );

        PaymentSimulationResponse response =
                paymentService.simulatePayment(
                        "TXN-TEST-456",
                        request,
                        "customer@example.com"
                );

        assertNotNull(response);

        assertEquals(
                PaymentStatus.FAILED,
                response.payment().status()
        );

        assertEquals(
                "CARD_DECLINED",
                response.payment().failureCode()
        );

        assertEquals(
                BookingStatus.PAYMENT_FAILED,
                response.bookingStatus()
        );

        assertEquals(HoldStatus.RELEASED, hold.getStatus());

        assertTrue(response.tickets().isEmpty());

        verify(ticketRepository, never()).saveAll(anyList());

        verifyNoInteractions(outboxService);
    }
}
