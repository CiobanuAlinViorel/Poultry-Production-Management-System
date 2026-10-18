package com.example.identity_service.dtos;

import jakarta.validation.constraints.NotBlank;

public record ClientCredentialsRequest(
        @NotBlank String clientId,
        @NotBlank String clientSecret
) {
}
