package kz.kimep.ticketing.catalog.api;

import kz.kimep.ticketing.catalog.api.dto.VenueRequest;
import kz.kimep.ticketing.catalog.api.dto.VenueResponse;
import kz.kimep.ticketing.catalog.service.VenueService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/venues")
public class VenueController {

    private final VenueService venueService;

    public VenueController(VenueService venueService) {
        this.venueService = venueService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")
    public ResponseEntity<VenueResponse> create(
            @Valid @RequestBody VenueRequest request
    ) {
        VenueResponse response = venueService.create(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<VenueResponse>> getAll() {

        return ResponseEntity.ok(
                venueService.getAll()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<VenueResponse> getById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                venueService.getById(id)
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")
    public ResponseEntity<VenueResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody VenueRequest request
    ) {
        return ResponseEntity.ok(
                venueService.update(id, request)
        );
    }
}
