package kz.kimep.ticketing.catalog.api.dto;

import kz.kimep.ticketing.catalog.domain.Venue;

public record VenueResponse(
        Long id,
        String name,
        String addressLine,
        String city,
        String timezone,
        Integer totalCapacity
) {

    public static VenueResponse from(Venue venue) {
        return new VenueResponse(
                venue.getId(),
                venue.getName(),
                venue.getAddressLine(),
                venue.getCity(),
                venue.getTimezone(),
                venue.getTotalCapacity()
        );
    }
}
