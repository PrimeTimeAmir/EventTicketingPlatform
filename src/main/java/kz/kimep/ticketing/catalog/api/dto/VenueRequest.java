package kz.kimep.ticketing.catalog.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record VenueRequest(

        @NotBlank
        @Size(max = 150)
        String name,

        @NotBlank
        String addressLine,

        @NotBlank
        @Size(max = 100)
        String city,

        @NotBlank
        @Size(max = 64)
        String timezone,

        @NotNull
        @Min(1)
        Integer totalCapacity

) {
}
