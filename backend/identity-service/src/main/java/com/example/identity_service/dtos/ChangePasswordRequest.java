package com.example.identity_service.dtos;

import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(
        @NotBlank
        String currentPassword,

        @NotBlank
        String newPassword        // password policy validated in PasswordPolicyService
) {}