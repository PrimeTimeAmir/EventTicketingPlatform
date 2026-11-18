package kz.kimep.ticketing.booking;

import org.springframework.test.util.ReflectionTestUtils;
import kz.kimep.ticketing.support.audit.service.AuditService;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import kz.kimep.ticketing.booking.api.dto.BookingRequest;
import kz.kimep.ticketing.booking.api.dto.BookingResponse;
import kz.kimep.ticketing.booking.domain.Booking;
import kz.kimep.ticketing.booking.domain.BookingStatus;
import kz.kimep.ticketing.booking.domain.HoldStatus;
import kz.kimep.ticketing.booking.domain.SeatHold;
import kz.kimep.ticketing.booking.repo.BookingRepository;
import kz.kimep.ticketing.booking.repo.SeatHoldRepository;
import kz.kimep.ticketing.booking.service.BookingService;
import kz.kimep.ticketing.catalog.domain.Event;
import kz.kimep.ticketing.catalog.domain.EventSectionPrice;
import kz.kimep.ticketing.catalog.domain.EventStatus;
import kz.kimep.ticketing.catalog.domain.Seat;
import kz.kimep.ticketing.catalog.domain.Section;
import kz.kimep.ticketing.catalog.domain.Venue;
import kz.kimep.ticketing.catalog.repo.EventRepository;
import kz.kimep.ticketing.catalog.repo.EventSectionPriceRepository;
import kz.kimep.ticketing.catalog.repo.SeatRepository;
import kz.kimep.ticketing.identity.domain.User;
import kz.kimep.ticketing.identity.repo.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private AuditService auditService;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private SeatHoldRepository seatHoldRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private EventSectionPriceRepository priceRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BookingService bookingService;

    @Test
    void shouldRejectBookingWhenHoldBelongsToAnotherUser() {

        User user = mock(User.class);

        when(userRepository.findByEmail("customer@example.com"))
                .thenReturn(Optional.of(user));

        when(user.getId()).thenReturn(7L);

        SeatHold hold = mock(SeatHold.class);

        when(seatHoldRepository.findByHoldReferenceForUpdate("hold-123"))
                .thenReturn(Optional.of(hold));

        // The hold belongs to another user
        when(hold.getUserId()).thenReturn(8L);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.createBooking(
                        new BookingRequest("hold-123"),
                        "customer@example.com"
                )
        );

        assertEquals(
                HttpStatus.FORBIDDEN.value(),
                exception.getStatusCode().value()
        );

        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void shouldRejectBookingWhenHoldHasExpired() {

        User user = mock(User.class);

        when(userRepository.findByEmail("customer@example.com"))
                .thenReturn(Optional.of(user));

        when(user.getId()).thenReturn(7L);

        SeatHold hold = mock(SeatHold.class);

        when(seatHoldRepository.findByHoldReferenceForUpdate("hold-123"))
                .thenReturn(Optional.of(hold));

        when(hold.getUserId()).thenReturn(7L);
        when(hold.getStatus()).thenReturn(HoldStatus.ACTIVE);
        when(hold.getExpiresAt())
                .thenReturn(OffsetDateTime.now().minusMinutes(1));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.createBooking(
                        new BookingRequest("hold-123"),
                        "customer@example.com"
                )
        );

        assertEquals(
                HttpStatus.CONFLICT.value(),
                exception.getStatusCode().value()
        );

        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void shouldCreatePendingBookingAndCalculateTotal() {

        OffsetDateTime now = OffsetDateTime.now();

        // User
        User user = mock(User.class);

        when(userRepository.findByEmail("customer@example.com"))
                .thenReturn(Optional.of(user));

        when(user.getId()).thenReturn(7L);

        // Active seat hold
        SeatHold hold = mock(SeatHold.class);

        when(seatHoldRepository.findByHoldReferenceForUpdate("hold-123"))
                .thenReturn(Optional.of(hold));

        when(hold.getId()).thenReturn(100L);
        when(hold.getUserId()).thenReturn(7L);
        when(hold.getStatus()).thenReturn(HoldStatus.ACTIVE);
        when(hold.getExpiresAt()).thenReturn(now.plusMinutes(10));
        when(hold.getEventId()).thenReturn(20L);
        when(hold.getHoldReference()).thenReturn("hold-123");
        when(hold.getSeatIds()).thenReturn(Set.of(1L, 2L));

        when(bookingRepository.existsBySeatHold_Id(100L))
                .thenReturn(false);

        // Published event
        Event event = mock(Event.class);

        when(eventRepository.findById(20L))
                .thenReturn(Optional.of(event));

        // Mock event ID
        when(event.getId()).thenReturn(20L);

        when(event.getStatus()).thenReturn(EventStatus.PUBLISHED);
        when(event.getSalesOpenAt()).thenReturn(now.minusHours(1));
        when(event.getSalesCloseAt()).thenReturn(now.plusDays(1));

        // Venue and section
        Venue venue = mock(Venue.class);
        Section section = mock(Section.class);

        when(venue.getId()).thenReturn(10L);
        when(section.getId()).thenReturn(30L);
        when(section.getVenue()).thenReturn(venue);
        when(event.getVenue()).thenReturn(venue);

        // Two selected seats
        Seat seat1 = mock(Seat.class);
        Seat seat2 = mock(Seat.class);

        when(seat1.getSection()).thenReturn(section);
        when(seat2.getSection()).thenReturn(section);

        when(seatRepository.findAllByIdInForUpdate(Set.of(1L, 2L)))
                .thenReturn(List.of(seat1, seat2));

        // Price for VIP section
        EventSectionPrice sectionPrice = mock(EventSectionPrice.class);

        when(sectionPrice.getSection()).thenReturn(section);
        when(sectionPrice.getPrice())
                .thenReturn(new BigDecimal("15000.00"));

        when(priceRepository.findAllByEvent_Id(20L))
                .thenReturn(List.of(sectionPrice));

        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(invocation -> {
                    Booking savedBooking = invocation.getArgument(0);

                    ReflectionTestUtils.setField(
                            savedBooking,
                            "id",
                            1L
                    );

                    return savedBooking;
                });

        // Execute service
        BookingResponse response = bookingService.createBooking(
                new BookingRequest("hold-123"),
                "customer@example.com"
        );

        // Verify response
        assertNotNull(response);

        assertEquals(20L, response.eventId());
        assertEquals(Set.of(1L, 2L), response.seatIds());

        assertEquals(BookingStatus.PENDING, response.status());

        assertEquals(
                0,
                new BigDecimal("30000.00")
                        .compareTo(response.subtotal())
        );

        assertEquals(
                0,
                BigDecimal.ZERO.compareTo(response.fees())
        );

        assertEquals(
                0,
                new BigDecimal("30000.00")
                        .compareTo(response.total())
        );

        assertEquals("KZT", response.currency());

        // Verify saved booking
        ArgumentCaptor<Booking> captor =
                ArgumentCaptor.forClass(Booking.class);

        verify(bookingRepository).save(captor.capture());

        Booking savedBooking = captor.getValue();

        assertEquals(
                BookingStatus.PENDING,
                savedBooking.getStatus()
        );

        assertEquals(7L, savedBooking.getUserId());
        assertEquals(20L, savedBooking.getEventId());

        // Verify repositories were called
        verify(seatHoldRepository)
                .findByHoldReferenceForUpdate("hold-123");

        verify(seatRepository)
                .findAllByIdInForUpdate(Set.of(1L, 2L));
    }
}
