package kz.kimep.ticketing.payment.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PaymentSimulationRequest(

        @NotNull
        Boolean successful,

        @Size(max = 100)
        String failureCode

)
{
}