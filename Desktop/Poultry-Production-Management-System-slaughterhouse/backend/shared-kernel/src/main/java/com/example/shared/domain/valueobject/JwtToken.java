package com.example.shared.domain.valueobject;

import com.example.shared.domain.exception.InvalidTokenException;
import lombok.Value;

import java.time.Instant;

@Value
public class JwtToken {
    String token;
    Instant issuedAt;
    Instant expiresAt;
    String tokenId;
    Long userId;
    String username;

    public JwtToken(String token, Instant issuedAt, Instant expiresAt, String tokenId, Long userId, String username) throws InvalidTokenException {
        if (token == null || token.isBlank()) {
            throw new InvalidTokenException("Token cannot be null or empty");
        }
        if (expiresAt.isBefore(Instant.now())) {
            throw new InvalidTokenException("Token has expired");
        }
        this.token = token;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.tokenId = tokenId;
        this.userId = userId;
        this.username = username;
    }

    public static JwtToken createAccessToken(Long userId, String username, String tokenId, Instant expiresAt) throws InvalidTokenException {
        return new JwtToken(
                generateTokenString(userId, username, expiresAt),
                Instant.now(),
                expiresAt,
                tokenId,
                userId,
                username
        );
    }

    public static JwtToken createAccessToken(Long userId, String tokenId) throws InvalidTokenException {
        // Default expiration: 1 hour
        Instant expiresAt = Instant.now().plusSeconds(3600);
        return createAccessToken(userId, " ", tokenId, expiresAt);
    }

    public boolean isValid() {
        return expiresAt.isAfter(Instant.now());
    }

    public boolean isExpired() {
        return !isValid();
    }

    private static String generateTokenString(Long userId, String username, Instant expiresAt) {
        // Implementation will be provided by infrastructure layer
        return "jwt." + userId + "." + username + "." + expiresAt.toEpochMilli();
    }
}