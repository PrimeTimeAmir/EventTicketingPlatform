package kz.kimep.ticketing.catalog.service;

import kz.kimep.ticketing.catalog.api.dto.VenueRequest;
import kz.kimep.ticketing.catalog.api.dto.VenueResponse;
import kz.kimep.ticketing.catalog.domain.Venue;
import kz.kimep.ticketing.catalog.repo.VenueRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.List;

@Service
public class VenueService {

    private final VenueRepository venueRepository;

    public VenueService(VenueRepository venueRepository) {
        this.venueRepository = venueRepository;
    }

    @Transactional
    public VenueResponse create(VenueRequest request) {

        validateTimezone(request.timezone());

        Venue venue = new Venue(
                request.name().trim(),
                request.addressLine().trim(),
                request.city().trim(),
                request.timezone().trim(),
                request.totalCapacity()
        );

        Venue savedVenue = venueRepository.save(venue);

        return VenueResponse.from(savedVenue);
    }

    @Transactional(readOnly = true)
    public List<VenueResponse> getAll() {

        return venueRepository.findAll()
                .stream()
                .map(VenueResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public VenueResponse getById(Long id) {

        Venue venue = findVenue(id);

        return VenueResponse.from(venue);
    }

    @Transactional
    public VenueResponse update(
            Long id,
            VenueRequest request
    ) {

        validateTimezone(request.timezone());

        Venue venue = findVenue(id);

        venue.setName(request.name().trim());
        venue.setAddressLine(request.addressLine().trim());
        venue.setCity(request.city().trim());
        venue.setTimezone(request.timezone().trim());
        venue.setTotalCapacity(request.totalCapacity());

        return VenueResponse.from(venue);
    }

    private Venue findVenue(Long id) {

        return venueRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Venue not found: " + id
                ));
    }

    private void validateTimezone(String timezone) {

        try {
            ZoneId.of(timezone.trim());

        } catch (DateTimeException exception) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid timezone: " + timezone
            );
        }
    }
}
