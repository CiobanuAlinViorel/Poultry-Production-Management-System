package com.example.identity_service.dtos;

import jakarta.validation.constraints.NotBlank;

public record ActivationRequest(
        @NotBlank String token,
        @NotBlank String newPassword
) {
}
