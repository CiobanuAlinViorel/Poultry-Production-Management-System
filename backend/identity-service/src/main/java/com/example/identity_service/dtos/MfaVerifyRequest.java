package com.example.identity_service.dtos;

import jakarta.validation.constraints.NotBlank;

public record MfaVerifyRequest(
        @NotBlank String mfaToken,
        @NotBlank String code
) {
}
