package kz.kimep.ticketing.catalog.service;

import kz.kimep.ticketing.catalog.api.dto.SectionRequest;
import kz.kimep.ticketing.catalog.api.dto.SectionResponse;
import kz.kimep.ticketing.catalog.domain.Section;
import kz.kimep.ticketing.catalog.domain.Seat;
import kz.kimep.ticketing.catalog.domain.Venue;
import kz.kimep.ticketing.catalog.repo.SectionRepository;
import kz.kimep.ticketing.catalog.repo.SeatRepository;
import kz.kimep.ticketing.catalog.repo.VenueRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.ArrayList;
import java.util.List;

@Service
public class SectionService {

    private final SectionRepository sectionRepository;
    private final SeatRepository seatRepository;
    private final VenueRepository venueRepository;

    public SectionService(
            SectionRepository sectionRepository,
            SeatRepository seatRepository,
            VenueRepository venueRepository
    ) {
        this.sectionRepository = sectionRepository;
        this.seatRepository = seatRepository;
        this.venueRepository = venueRepository;
    }

    // Create section and automatically generate seats
    @Transactional
    public SectionResponse createSection(
            Long venueId,
            SectionRequest request
    ) {

        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Venue not found: " + venueId
                ));

        long requestedSeats =
                (long) request.rowCount() * request.seatsPerRow();

        // Calculate capacity already occupied by existing sections.
        long currentCapacity = sectionRepository
                .findAllByVenue_Id(venueId)
                .stream()
                .mapToLong(section ->
                        (long) section.getRowCount()
                                * section.getSeatsPerRow()
                )
                .sum();

        // Prevent exceeding the venue capacity.
        if (currentCapacity + requestedSeats > venue.getTotalCapacity()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Section capacity exceeds venue capacity. "
                            + "Available seats: "
                            + (venue.getTotalCapacity() - currentCapacity)
            );
        }

        Section section = new Section(
                venue,
                request.name().trim(),
                request.rowCount(),
                request.seatsPerRow(),
                request.priceTier().trim()
        );

        Section savedSection = sectionRepository.save(section);

        // Generate all seats automatically.
        List<Seat> seats = new ArrayList<>((int) requestedSeats);

        for (int row = 0; row < request.rowCount(); row++) {

            String rowLabel = toRowLabel(row);

            for (int seatNumber = 1;
                 seatNumber <= request.seatsPerRow();
                 seatNumber++) {

                seats.add(new Seat(
                        savedSection,
                        rowLabel,
                        seatNumber
                ));
            }
        }

        seatRepository.saveAll(seats);

        return SectionResponse.from(savedSection);
    }

    // Get all sections belonging to a venue
    @Transactional(readOnly = true)
    public List<SectionResponse> getSectionsByVenue(Long venueId) {

        if (!venueRepository.existsById(venueId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Venue not found: " + venueId
            );
        }

        return sectionRepository.findAllByVenue_Id(venueId)
                .stream()
                .map(SectionResponse::from)
                .toList();
    }

    // Convert row number into Excel-style row label:
    // 0 -> A, 1 -> B, ..., 25 -> Z, 26 -> AA.
    private String toRowLabel(int rowIndex) {

        StringBuilder label = new StringBuilder();
        int value = rowIndex + 1;

        while (value > 0) {
            value--;

            label.insert(
                    0,
                    (char) ('A' + value % 26)
            );

            value /= 26;
        }

        return label.toString();
    }
}
