package kz.kimep.ticketing.catalog.api;

import kz.kimep.ticketing.catalog.api.dto.SeatResponse;
import kz.kimep.ticketing.catalog.service.SeatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/sections/{sectionId}/seats")
public class SeatController {

    private final SeatService seatService;

    public SeatController(SeatService seatService) {
        this.seatService = seatService;
    }

    @GetMapping
    public ResponseEntity<List<SeatResponse>> getSeats(
            @PathVariable Long sectionId
    ) {
        return ResponseEntity.ok(
                seatService.getSeatsBySection(sectionId)
        );
    }
}