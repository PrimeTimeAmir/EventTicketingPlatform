package kz.kimep.ticketing.catalog.api;

import jakarta.validation.Valid;
import kz.kimep.ticketing.catalog.api.dto.EventRequest;
import kz.kimep.ticketing.catalog.api.dto.EventResponse;
import kz.kimep.ticketing.catalog.domain.EventStatus;
import kz.kimep.ticketing.catalog.service.EventService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import kz.kimep.ticketing.catalog.api.dto.EventStatusRequest;

@RestController
@RequestMapping("/api/v1/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")
    public ResponseEntity<EventResponse> create(
            @Valid @RequestBody EventRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {

        EventResponse response = eventService.create(
                request,
                userDetails.getUsername()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<Page<EventResponse>> getAll(
            @RequestParam(required = false) EventStatus status,
            @PageableDefault(
                    size = 20,
                    sort = "startsAt",
                    direction = Sort.Direction.ASC
            ) Pageable pageable
    ) {

        return ResponseEntity.ok(
                eventService.getAll(status, pageable)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getById(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                eventService.getById(id)
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")
    public ResponseEntity<EventResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody EventRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {

        return ResponseEntity.ok(
                eventService.update(
                        id,
                        request,
                        userDetails.getUsername()
                )
        );
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")
    public ResponseEntity<EventResponse> changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody EventStatusRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {

        return ResponseEntity.ok(
                eventService.changeStatus(
                        id,
                        request.status(),
                        userDetails.getUsername()
                )
        );
    }
}