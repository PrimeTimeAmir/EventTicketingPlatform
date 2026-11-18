package kz.kimep.ticketing.catalog.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

public record EventRequest(

        @NotNull
        @Positive
        Long venueId,

        @NotBlank
        @Size(max = 200)
        String title,

        String description,

        @NotNull
        OffsetDateTime startsAt,

        @NotNull
        OffsetDateTime doorsOpenAt,

        @NotNull
        OffsetDateTime salesOpenAt,

        @NotNull
        OffsetDateTime salesCloseAt

) {
}