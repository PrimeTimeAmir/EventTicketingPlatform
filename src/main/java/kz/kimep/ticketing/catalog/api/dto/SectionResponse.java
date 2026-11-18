package kz.kimep.ticketing.catalog.api.dto;

import kz.kimep.ticketing.catalog.domain.Section;

public record SectionResponse(

        Long id,
        Long venueId,
        String name,
        Integer rowCount,
        Integer seatsPerRow,
        Integer totalSeats,
        String priceTier

) {

    public static SectionResponse from(Section section) {

        return new SectionResponse(
                section.getId(),
                section.getVenue().getId(),
                section.getName(),
                section.getRowCount(),
                section.getSeatsPerRow(),
                section.getRowCount() * section.getSeatsPerRow(),
                section.getPriceTier()
        );
    }
}