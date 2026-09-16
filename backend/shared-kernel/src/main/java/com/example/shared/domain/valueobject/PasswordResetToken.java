package com.example.shared.domain.valueobject;

import com.example.shared.domain.exception.InvalidTokenException;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
public class PasswordResetToken {
    String token;
    Instant createdAt;
    Instant expiresAt;
    String email;

    public PasswordResetToken(String token, Instant createdAt, Instant expiresAt, String email) throws InvalidTokenException {
        if (token == null || token.isBlank()) {
            throw new InvalidTokenException("Password reset token cannot be null or empty");
        }
        if (expiresAt.isBefore(Instant.now())) {
            throw new InvalidTokenException("Password reset token has expired");
        }
        this.token = token;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.email = email;
    }

    public static PasswordResetToken generate(String email) throws InvalidTokenException {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(24 * 60 * 60); // 24 hours

        return new PasswordResetToken(
                UUID.randomUUID().toString().replace("-", "").substring(0, 8),
                now,
                expiresAt,
                email
        );
    }

    public boolean isValid() {
        return expiresAt.isAfter(Instant.now());
    }

    public boolean isExpired() {
        return !isValid();
    }

    public boolean matches(String tokenToVerify) {
        return this.token.equals(tokenToVerify);
    }
}