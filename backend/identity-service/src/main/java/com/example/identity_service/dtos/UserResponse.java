package com.example.identity_service.dtos;

import com.example.identity_service.domain.UserStatus;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String username,
        String displayName,
        String phone,
        String locale,
        UserStatus status,
        UUID employeeId,
        Instant lastLoginAt,
        Instant createdAt
) {
}
