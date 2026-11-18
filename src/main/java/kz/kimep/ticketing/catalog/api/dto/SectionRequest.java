package kz.kimep.ticketing.catalog.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SectionRequest(

        @NotBlank
        @Size(max = 100)
        String name,

        @NotNull
        @Min(1)
        @Max(500)
        Integer rowCount,

        @NotNull
        @Min(1)
        @Max(500)
        Integer seatsPerRow,

        @NotBlank
        @Size(max = 50)
        String priceTier

) {
}