package kz.kimep.ticketing.booking.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BookingRequest(

        @NotBlank
        @Size(max = 64)
        String holdReference

)
{
}