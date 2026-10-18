package com.example.identity_service.dtos;

import jakarta.validation.constraints.NotBlank;

public record PinLoginRequest(
        @NotBlank String username,
        @NotBlank String pin
) {
}
