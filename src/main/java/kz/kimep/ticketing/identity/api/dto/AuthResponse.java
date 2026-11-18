package kz.kimep.ticketing.identity.api.dto;

public record AuthResponse(
        String accessToken,
        String tokenType,
        String refreshToken
) {
}
