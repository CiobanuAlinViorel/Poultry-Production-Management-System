package com.example.identity_service.dtos;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        boolean mfaRequired,
        String mfaToken,
        String tokenType,
        long expiresIn
) {
    public TokenResponse(String accessToken, String refreshToken, boolean mfaRequired, String mfaToken) {
        this(accessToken, refreshToken, mfaRequired, mfaToken, "Bearer", 900L);
    }

    public TokenResponse(String accessToken, String refreshToken) {
        this(accessToken, refreshToken, false, null, "Bearer", 900L);
    }
}
