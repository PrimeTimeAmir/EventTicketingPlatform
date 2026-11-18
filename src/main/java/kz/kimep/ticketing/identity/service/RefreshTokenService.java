package kz.kimep.ticketing.identity.service;

import kz.kimep.ticketing.identity.domain.RefreshToken;
import kz.kimep.ticketing.identity.domain.User;
import kz.kimep.ticketing.identity.repo.RefreshTokenRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class RefreshTokenService {

    private static final SecureRandom SECURE_RANDOM =
            new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final long refreshTokenExpirationMs;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            @Value("${app.jwt.refresh-token-expiration-ms}")
            long refreshTokenExpirationMs
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    @Transactional
    public String createRefreshToken(User user) {

        String rawToken = generateRawToken();

        String tokenHash = hashToken(rawToken);

        OffsetDateTime expiresAt = OffsetDateTime.now()
                .plusNanos(refreshTokenExpirationMs * 1_000_000L);

        RefreshToken refreshToken = new RefreshToken(
                user,
                tokenHash,
                expiresAt
        );

        refreshTokenRepository.save(refreshToken);

        return rawToken;
    }

    @Transactional
    public RotationResult rotateRefreshToken(String rawToken) {

        if (rawToken == null || rawToken.isBlank()) {
            throw invalidRefreshToken();
        }

        String tokenHash = hashToken(rawToken);

        RefreshToken currentToken = refreshTokenRepository
                .findByTokenHashForUpdate(tokenHash)
                .orElseThrow(this::invalidRefreshToken);

        OffsetDateTime now = OffsetDateTime.now();

        if (currentToken.isRevoked()
                || !currentToken.getExpiresAt().isAfter(now)
                || !currentToken.getUser().isEnabled()) {

            throw invalidRefreshToken();
        }

        // Revoke the old token.
        currentToken.setRevoked(true);
        refreshTokenRepository.save(currentToken);

        // Generate a completely new token.
        String newRawToken = generateRawToken();
        String newTokenHash = hashToken(newRawToken);

        RefreshToken newRefreshToken = new RefreshToken(
                currentToken.getUser(),
                newTokenHash,
                now.plusNanos(
                        refreshTokenExpirationMs * 1_000_000L
                )
        );

        refreshTokenRepository.save(newRefreshToken);

        return new RotationResult(
                newRawToken,
                currentToken.getUser().getEmail()
        );
    }

    public record RotationResult(
            String rawToken,
            String email
    ) {
    }

    private String generateRawToken() {

        byte[] randomBytes = new byte[32];

        SECURE_RANDOM.nextBytes(randomBytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
    }

    private String hashToken(String rawToken) {

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    rawToken.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 algorithm is unavailable",
                    exception
            );
        }
    }

    private ResponseStatusException invalidRefreshToken() {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Invalid or expired refresh token"
        );
    }

    @Transactional
    public void revokeRefreshToken(String rawToken) {

        if (rawToken == null || rawToken.isBlank()) {
            return;
        }

        String tokenHash = hashToken(rawToken);

        refreshTokenRepository
                .findByTokenHashForUpdate(tokenHash)
                .ifPresent(token -> token.setRevoked(true));
    }

}
