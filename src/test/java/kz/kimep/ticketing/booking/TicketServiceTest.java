package kz.kimep.ticketing.booking;

import kz.kimep.ticketing.booking.api.dto.TicketDetailsResponse;
import kz.kimep.ticketing.booking.domain.Booking;
import kz.kimep.ticketing.support.audit.service.AuditService;
import kz.kimep.ticketing.booking.domain.Ticket;
import kz.kimep.ticketing.booking.domain.TicketStatus;
import kz.kimep.ticketing.booking.repo.TicketRepository;
import kz.kimep.ticketing.booking.service.TicketService;
import kz.kimep.ticketing.catalog.domain.Event;
import kz.kimep.ticketing.catalog.domain.EventStatus;
import kz.kimep.ticketing.catalog.repo.EventRepository;
import kz.kimep.ticketing.identity.domain.Role;
import kz.kimep.ticketing.identity.domain.User;
import kz.kimep.ticketing.identity.repo.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private AuditService auditService;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventRepository eventRepository;

    private TicketService ticketService;

    @BeforeEach
    void setUp() {
        ticketService = new TicketService(
                ticketRepository,
                userRepository,
                eventRepository,
                auditService
        );
    }


    // Helper: create user
    private User createUser(String email, Long id, Role role) {
        User user = mock(User.class);

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        lenient().when(user.getId()).thenReturn(id);
        lenient().when(user.getRole()).thenReturn(role);

        return user;
    }

    // Helper: create ticket
    private Ticket createTicket(Long eventId, TicketStatus status) {
        Ticket ticket = mock(Ticket.class);

        lenient().when(ticket.getEventId()).thenReturn(eventId);
        lenient().when(ticket.getStatus()).thenReturn(status);

        return ticket;
    }

    // Helper: create event
    private Event createEvent(
            Long eventId,
            User organizer,
            EventStatus status
    ) {
        Event event = mock(Event.class);

        when(eventRepository.findById(eventId))
                .thenReturn(Optional.of(event));

        lenient().when(event.getOrganizer()).thenReturn(organizer);
        lenient().when(event.getStatus()).thenReturn(status);

        return event;
    }

    // TEST 1: Get all tickets belonging to user
    @Test
    void shouldReturnMyTickets() {
        String email = "customer@example.com";
        Long userId = 10L;

        createUser(email, userId, Role.CUSTOMER);

        Ticket ticket = mock(Ticket.class);
        TicketDetailsResponse expectedResponse =
                mock(TicketDetailsResponse.class);

        when(ticketRepository
                .findAllByBooking_UserIdOrderByIssuedAtDesc(userId))
                .thenReturn(List.of(ticket));

        try (MockedStatic<TicketDetailsResponse> mapper =
                     mockStatic(TicketDetailsResponse.class)) {

            mapper.when(() -> TicketDetailsResponse.from(ticket))
                    .thenReturn(expectedResponse);

            List<TicketDetailsResponse> result =
                    ticketService.getMyTickets(email);

            assertEquals(1, result.size());
            assertSame(expectedResponse, result.get(0));
        }

        verify(ticketRepository)
                .findAllByBooking_UserIdOrderByIssuedAtDesc(userId);
    }

    // TEST 2: Get ticket by code when user owns it
    @Test
    void shouldReturnTicketWhenUserOwnsIt() {
        String email = "customer@example.com";
        String ticketCode = "TKT-123";
        Long userId = 10L;

        createUser(email, userId, Role.CUSTOMER);

        Ticket ticket = mock(Ticket.class);
        Booking booking = mock(Booking.class);

        when(ticketRepository.findByTicketCode(ticketCode))
                .thenReturn(Optional.of(ticket));

        when(ticket.getBooking()).thenReturn(booking);
        when(booking.getUserId()).thenReturn(userId);

        TicketDetailsResponse expectedResponse =
                mock(TicketDetailsResponse.class);

        try (MockedStatic<TicketDetailsResponse> mapper =
                     mockStatic(TicketDetailsResponse.class)) {

            mapper.when(() -> TicketDetailsResponse.from(ticket))
                    .thenReturn(expectedResponse);

            TicketDetailsResponse result =
                    ticketService.getTicketByCode(ticketCode, email);

            assertSame(expectedResponse, result);
        }
    }

    // TEST 3: Prevent user from viewing another user's ticket
    @Test
    void shouldRejectTicketWhenItBelongsToAnotherUser() {
        String email = "customer@example.com";
        String ticketCode = "TKT-123";

        createUser(email, 10L, Role.CUSTOMER);

        Ticket ticket = mock(Ticket.class);
        Booking booking = mock(Booking.class);

        when(ticketRepository.findByTicketCode(ticketCode))
                .thenReturn(Optional.of(ticket));

        when(ticket.getBooking()).thenReturn(booking);
        when(booking.getUserId()).thenReturn(20L);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> ticketService.getTicketByCode(ticketCode, email)
        );

        assertEquals(
                HttpStatus.FORBIDDEN.value(),
                exception.getStatusCode().value()
        );
    }

    // TEST 4: Organizer can check in a ticket
    @Test
    void shouldAllowOrganizerToCheckInTicket() {
        String email = "organizer@example.com";
        String ticketCode = "TKT-123";
        Long organizerId = 20L;
        Long eventId = 30L;

        User organizer = createUser(email, organizerId, Role.ORGANIZER);

        Ticket ticket = createTicket(eventId, TicketStatus.ISSUED);

        when(ticketRepository.findByTicketCodeForUpdate(ticketCode))
                .thenReturn(Optional.of(ticket));

        createEvent(eventId, organizer, EventStatus.PUBLISHED);

        TicketDetailsResponse expectedResponse =
                mock(TicketDetailsResponse.class);

        try (MockedStatic<TicketDetailsResponse> mapper =
                     mockStatic(TicketDetailsResponse.class)) {

            mapper.when(() -> TicketDetailsResponse.from(ticket))
                    .thenReturn(expectedResponse);

            TicketDetailsResponse result =
                    ticketService.checkInTicket(ticketCode, email);

            assertSame(expectedResponse, result);
        }

        verify(ticket).setStatus(TicketStatus.CHECKED_IN);

        ArgumentCaptor<OffsetDateTime> timeCaptor =
                ArgumentCaptor.forClass(OffsetDateTime.class);

        verify(ticket).setCheckedInAt(timeCaptor.capture());

        assertNotNull(timeCaptor.getValue());
    }

    // TEST 5: Non-organizer customer cannot check in a ticket
    @Test
    void shouldRejectCheckInByNonOrganizer() {
        String email = "customer@example.com";
        String ticketCode = "TKT-123";
        Long eventId = 30L;

        User customer = createUser(email, 10L, Role.CUSTOMER);
        User organizer = mock(User.class);

        when(organizer.getId()).thenReturn(20L);

        Ticket ticket = createTicket(eventId, TicketStatus.ISSUED);

        when(ticketRepository.findByTicketCodeForUpdate(ticketCode))
                .thenReturn(Optional.of(ticket));

        createEvent(eventId, organizer, EventStatus.PUBLISHED);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> ticketService.checkInTicket(ticketCode, email)
        );

        assertEquals(
                HttpStatus.FORBIDDEN.value(),
                exception.getStatusCode().value()
        );

        verify(ticket, never()).setStatus(any());
        verify(ticket, never()).setCheckedInAt(any());
    }

    // TEST 6: Cannot check in tickets for cancelled event
    @Test
    void shouldRejectCheckInForCancelledEvent() {
        String email = "organizer@example.com";
        String ticketCode = "TKT-123";
        Long organizerId = 20L;
        Long eventId = 30L;

        User organizer = createUser(email, organizerId, Role.ORGANIZER);

        Ticket ticket = createTicket(eventId, TicketStatus.ISSUED);

        when(ticketRepository.findByTicketCodeForUpdate(ticketCode))
                .thenReturn(Optional.of(ticket));

        createEvent(eventId, organizer, EventStatus.CANCELLED);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> ticketService.checkInTicket(ticketCode, email)
        );

        assertEquals(
                HttpStatus.CONFLICT.value(),
                exception.getStatusCode().value()
        );

        verify(ticket, never()).setStatus(any());
    }

    // TEST 7: Cannot check in an already checked-in ticket
    @Test
    void shouldRejectRepeatedCheckIn() {
        String email = "organizer@example.com";
        String ticketCode = "TKT-123";
        Long organizerId = 20L;
        Long eventId = 30L;

        User organizer = createUser(email, organizerId, Role.ORGANIZER);

        Ticket ticket = createTicket(eventId, TicketStatus.CHECKED_IN);

        when(ticketRepository.findByTicketCodeForUpdate(ticketCode))
                .thenReturn(Optional.of(ticket));

        createEvent(eventId, organizer, EventStatus.PUBLISHED);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> ticketService.checkInTicket(ticketCode, email)
        );

        assertEquals(
                HttpStatus.CONFLICT.value(),
                exception.getStatusCode().value()
        );

        verify(ticket, never()).setStatus(any());
    }

    // TEST 8: Admin can check in a ticket
    @Test
    void shouldAllowAdminToCheckInTicket() {
        String email = "admin@example.com";
        String ticketCode = "TKT-123";
        Long eventId = 30L;

        User admin = createUser(email, 1L, Role.ADMIN);

        User organizer = mock(User.class);
        when(organizer.getId()).thenReturn(20L);

        Ticket ticket = createTicket(eventId, TicketStatus.ISSUED);

        when(ticketRepository.findByTicketCodeForUpdate(ticketCode))
                .thenReturn(Optional.of(ticket));

        createEvent(eventId, organizer, EventStatus.PUBLISHED);

        TicketDetailsResponse expectedResponse =
                mock(TicketDetailsResponse.class);

        try (MockedStatic<TicketDetailsResponse> mapper =
                     mockStatic(TicketDetailsResponse.class)) {

            mapper.when(() -> TicketDetailsResponse.from(ticket))
                    .thenReturn(expectedResponse);

            TicketDetailsResponse result =
                    ticketService.checkInTicket(ticketCode, email);

            assertSame(expectedResponse, result);
        }

        verify(ticket).setStatus(TicketStatus.CHECKED_IN);
    }

    // TEST 9: User does not exist
    @Test
    void shouldReturnNotFoundWhenUserDoesNotExist() {
        String email = "missing@example.com";

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> ticketService.getMyTickets(email)
        );

        assertEquals(
                HttpStatus.NOT_FOUND.value(),
                exception.getStatusCode().value()
        );

        verifyNoInteractions(ticketRepository);
    }

    // TEST 10: Ticket does not exist
    @Test
    void shouldReturnNotFoundWhenTicketDoesNotExist() {
        String email = "customer@example.com";
        String ticketCode = "TKT-MISSING";

        createUser(email, 10L, Role.CUSTOMER);

        when(ticketRepository.findByTicketCode(ticketCode))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> ticketService.getTicketByCode(ticketCode, email)
        );

        assertEquals(
                HttpStatus.NOT_FOUND.value(),
                exception.getStatusCode().value()
        );
    }
}
