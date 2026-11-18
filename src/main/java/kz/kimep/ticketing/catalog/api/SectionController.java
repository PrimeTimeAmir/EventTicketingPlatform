package kz.kimep.ticketing.catalog.api;

import jakarta.validation.Valid;
import kz.kimep.ticketing.catalog.api.dto.SectionRequest;
import kz.kimep.ticketing.catalog.api.dto.SectionResponse;
import kz.kimep.ticketing.catalog.service.SectionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/venues/{venueId}/sections")
public class SectionController {

    private final SectionService sectionService;

    public SectionController(SectionService sectionService) {
        this.sectionService = sectionService;
    }

    // Create a section and automatically generate seats
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")
    public ResponseEntity<SectionResponse> createSection(
            @PathVariable Long venueId,
            @Valid @RequestBody SectionRequest request
    ) {

        SectionResponse response = sectionService.createSection(
                venueId,
                request
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // Get all sections belonging to a venue
    @GetMapping
    public ResponseEntity<List<SectionResponse>> getSections(
            @PathVariable Long venueId
    ) {

        return ResponseEntity.ok(
                sectionService.getSectionsByVenue(venueId)
        );
    }
}
