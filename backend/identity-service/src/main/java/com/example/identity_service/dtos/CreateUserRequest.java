package com.example.identity_service.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record CreateUserRequest(
        @NotBlank @Email String email,
        String username,
        String displayName,
        UUID employeeId,
        String locale,
        boolean sendActivationEmail
) {
}
