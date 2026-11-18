package kz.kimep.ticketing.booking.api.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record SeatHoldRequest(

        @NotEmpty
        @Size(min = 1, max = 10)
        List<@NotNull @Positive Long> seatIds

)
{
}