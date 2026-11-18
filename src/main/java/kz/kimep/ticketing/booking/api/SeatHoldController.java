package kz.kimep.ticketing.booking.api;

import jakarta.validation.Valid;
import kz.kimep.ticketing.booking.api.dto.SeatHoldRequest;
import kz.kimep.ticketing.booking.api.dto.SeatHoldResponse;
import kz.kimep.ticketing.booking.service.SeatHoldService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/events/{eventId}/holds")
public class SeatHoldController {

    private final SeatHoldService seatHoldService;

    public SeatHoldController(SeatHoldService seatHoldService) {
        this.seatHoldService = seatHoldService;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SeatHoldResponse> createHold(
            @PathVariable Long eventId,
            @Valid @RequestBody SeatHoldRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {

        SeatHoldResponse response = seatHoldService.createHold(
                eventId,
                request,
                userDetails.getUsername()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}