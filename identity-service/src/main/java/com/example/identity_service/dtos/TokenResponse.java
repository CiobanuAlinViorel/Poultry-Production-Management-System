package com.example.identity_service.dtos;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,      // always "Bearer"
        long expiresIn         // access token TTL in seconds — always 900 (15 min)
) {
    // convenience constructor with defaults
    public TokenResponse(String accessToken, String refreshToken) {
        this(accessToken, refreshToken, "Bearer", 900L);
    }
}