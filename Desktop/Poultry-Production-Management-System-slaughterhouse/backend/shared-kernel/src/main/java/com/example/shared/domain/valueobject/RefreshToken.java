package com.example.shared.domain.valueobject;

import com.example.shared.domain.entity.User;
import com.example.shared.domain.exception.InvalidTokenException;
import lombok.Value;

import java.time.Instant;

@Value
public class RefreshToken {
    String token;
    Instant issuedAt;
    Instant expiresAt;
    String tokenId;
    Long userId;
    String username;

    public RefreshToken(String token, Instant issuedAt, Instant expiresAt, String tokenId, Long userId, String username) throws InvalidTokenException {
        if (token == null || token.isBlank()) {
            throw new InvalidTokenException("Refresh token cannot be null or empty");
        }
        if (expiresAt.isBefore(Instant.now())) {
            throw new InvalidTokenException("Refresh token has expired");
        }
        this.token = token;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.tokenId = tokenId;
        this.userId = userId;
        this.username = username;
    }

    public static RefreshToken create(Long userId, String username, String tokenId) throws InvalidTokenException {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(7 * 24 * 60 * 60); // 7 days

        return new RefreshToken(
                generateRefreshTokenString(userId, username, expiresAt),
                now,
                expiresAt,
                tokenId,
                userId,
                username
        );
    }

    public boolean isValid() {
        return !expiresAt.isAfter(Instant.now());
    }

    public boolean isExpired() {
        return isValid();
    }

    private static String generateRefreshTokenString(Long userId, String username, Instant expiresAt) {
        // Implementation will be provided by infrastructure layer
        return "refresh." + userId + "." + username + "." + expiresAt.toEpochMilli();
    }

    public Long getUser() {
        return userId;
    }
}