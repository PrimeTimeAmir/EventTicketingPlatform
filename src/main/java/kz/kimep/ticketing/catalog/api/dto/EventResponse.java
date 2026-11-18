package kz.kimep.ticketing.catalog.api.dto;

import kz.kimep.ticketing.catalog.domain.Event;
import kz.kimep.ticketing.catalog.domain.EventStatus;
import java.time.OffsetDateTime;

public record EventResponse(

        Long id,

        Long venueId,
        String venueName,

        Long organizerId,

        String title,
        String description,

        OffsetDateTime startsAt,
        OffsetDateTime doorsOpenAt,

        OffsetDateTime salesOpenAt,
        OffsetDateTime salesCloseAt,

        EventStatus status,

        OffsetDateTime createdAt

) {

    public static EventResponse from(Event event) {

        return new EventResponse(
                event.getId(),

                event.getVenue().getId(),
                event.getVenue().getName(),

                event.getOrganizer().getId(),

                event.getTitle(),
                event.getDescription(),

                event.getStartsAt(),
                event.getDoorsOpenAt(),

                event.getSalesOpenAt(),
                event.getSalesCloseAt(),

                event.getStatus(),

                event.getCreatedAt()
        );
    }
}