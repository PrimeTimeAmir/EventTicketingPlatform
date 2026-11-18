package kz.kimep.ticketing.booking.service;

import kz.kimep.ticketing.booking.api.dto.TicketDetailsResponse;
import kz.kimep.ticketing.support.audit.service.AuditService;
import kz.kimep.ticketing.booking.domain.Ticket;
import kz.kimep.ticketing.booking.domain.TicketStatus;
import kz.kimep.ticketing.booking.repo.TicketRepository;
import kz.kimep.ticketing.catalog.domain.Event;
import kz.kimep.ticketing.catalog.domain.EventStatus;
import kz.kimep.ticketing.catalog.repo.EventRepository;
import kz.kimep.ticketing.identity.domain.Role;
import kz.kimep.ticketing.identity.domain.User;
import kz.kimep.ticketing.identity.repo.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final AuditService auditService;

    public TicketService(
            TicketRepository ticketRepository,
            UserRepository userRepository,
            EventRepository eventRepository,
            AuditService auditService
    ) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.auditService = auditService;
    }

    // Get all tickets belonging to current user
    @Transactional(readOnly = true)
    public List<TicketDetailsResponse> getMyTickets(String userEmail) {

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));

        return ticketRepository
                .findAllByBooking_UserIdOrderByIssuedAtDesc(user.getId())
                .stream()
                .map(TicketDetailsResponse::from)
                .toList();
    }

    // Get ticket by unique code
    @Transactional(readOnly = true)
    public TicketDetailsResponse getTicketByCode(
            String ticketCode,
            String userEmail
    ) {

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));

        Ticket ticket = ticketRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Ticket not found"
                ));

        if (!ticket.getBooking().getUserId().equals(user.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "This ticket belongs to another user"
            );
        }

        return TicketDetailsResponse.from(ticket);
    }

    // Check in ticket
    @Transactional
    public TicketDetailsResponse checkInTicket(
            String ticketCode,
            String userEmail
    ) {

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));

        // Lock ticket to prevent simultaneous check-in
        Ticket ticket = ticketRepository
                .findByTicketCodeForUpdate(ticketCode)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Ticket not found"
                ));

        Event event = eventRepository.findById(ticket.getEventId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Event not found"
                ));

        // Only ADMIN or the event organizer can check in tickets
        boolean isAdmin = user.getRole() == Role.ADMIN;

        boolean isOrganizer = event.getOrganizer()
                .getId()
                .equals(user.getId());

        if (!isAdmin && !isOrganizer) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not allowed to check in this ticket"
            );
        }

        if (event.getStatus() == EventStatus.CANCELLED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot check in tickets for a cancelled event"
            );
        }

        if (ticket.getStatus() != TicketStatus.ISSUED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ticket is not available for check-in"
            );
        }

        ticket.setStatus(TicketStatus.CHECKED_IN);
        ticket.setCheckedInAt(OffsetDateTime.now());

        auditService.record(
                user.getId(),
                "TICKET_CHECKED_IN",
                "TICKET",
                ticketCode
        );

        return TicketDetailsResponse.from(ticket);
    }
}
