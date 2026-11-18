package kz.kimep.ticketing.catalog.api;

import jakarta.validation.Valid;
import kz.kimep.ticketing.catalog.api.dto.EventSectionPriceRequest;
import kz.kimep.ticketing.catalog.api.dto.EventSectionPriceResponse;
import kz.kimep.ticketing.catalog.service.EventSectionPriceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/events/{eventId}/prices")
public class EventSectionPriceController {

    private final EventSectionPriceService priceService;

    public EventSectionPriceController(
            EventSectionPriceService priceService
    ) {
        this.priceService = priceService;
    }

    // Assign a ticket price to an event section
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")
    public ResponseEntity<EventSectionPriceResponse> createPrice(
            @PathVariable Long eventId,
            @Valid @RequestBody EventSectionPriceRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {

        EventSectionPriceResponse response = priceService.createPrice(
                eventId,
                request,
                userDetails.getUsername()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // Get all ticket prices for an event
    @GetMapping
    public ResponseEntity<List<EventSectionPriceResponse>> getPrices(
            @PathVariable Long eventId
    ) {

        return ResponseEntity.ok(
                priceService.getPricesByEvent(eventId)
        );
    }
}
