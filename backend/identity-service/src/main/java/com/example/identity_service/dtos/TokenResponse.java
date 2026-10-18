package com.example.identity_service.dtos;

import java.util.List;
import java.util.UUID;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        boolean mfaRequired,
        String mfaToken,
        String tokenType,
        long expiresIn,
        AuthUser user
) {
    public TokenResponse(String accessToken, String refreshToken, boolean mfaRequired, String mfaToken) {
        this(accessToken, refreshToken, mfaRequired, mfaToken, "Bearer", 900L, null);
    }

    public TokenResponse(String accessToken, String refreshToken, AuthUser user) {
        this(accessToken, refreshToken, false, null, "Bearer", 900L, user);
    }

    public TokenResponse(String accessToken, String refreshToken) {
        this(accessToken, refreshToken, false, null, "Bearer", 900L, null);
    }

    public record AuthUser(UUID id, String email, String displayName, List<String> roles) {
    }
}
