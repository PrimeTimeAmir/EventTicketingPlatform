package kz.kimep.ticketing.identity.api.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(

        @NotBlank
        String refreshToken

) {
}
