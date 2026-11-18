package kz.kimep.ticketing.payment.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kz.kimep.ticketing.payment.domain.PaymentMethod;

public record PaymentRequest(

        @NotBlank
        @Size(max = 64)
        String bookingReference,

        @NotNull
        PaymentMethod method

)
{
}