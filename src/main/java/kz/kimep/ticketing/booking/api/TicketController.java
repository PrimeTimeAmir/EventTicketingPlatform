package kz.kimep.ticketing.booking.api;

import kz.kimep.ticketing.booking.api.dto.TicketDetailsResponse;
import kz.kimep.ticketing.booking.service.TicketService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    // Get current user's tickets
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TicketDetailsResponse>> getMyTickets(
            @AuthenticationPrincipal UserDetails userDetails
    ) {

        return ResponseEntity.ok(
                ticketService.getMyTickets(userDetails.getUsername())
        );
    }

    // Get ticket by code
    @GetMapping("/{ticketCode}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TicketDetailsResponse> getTicket(
            @PathVariable String ticketCode,
            @AuthenticationPrincipal UserDetails userDetails
    ) {

        return ResponseEntity.ok(
                ticketService.getTicketByCode(
                        ticketCode,
                        userDetails.getUsername()
                )
        );
    }

    // Check in ticket
    @PostMapping("/{ticketCode}/check-in")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")
    public ResponseEntity<TicketDetailsResponse> checkIn(
            @PathVariable String ticketCode,
            @AuthenticationPrincipal UserDetails userDetails
    ) {

        return ResponseEntity.ok(
                ticketService.checkInTicket(
                        ticketCode,
                        userDetails.getUsername()
                )
        );
    }
}