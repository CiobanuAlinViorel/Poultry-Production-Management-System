package com.example.identity_service.dtos;

public record UpdateProfileRequest(
        String displayName,
        String phone,
        String locale,
        String avatarUrl
) {
}
