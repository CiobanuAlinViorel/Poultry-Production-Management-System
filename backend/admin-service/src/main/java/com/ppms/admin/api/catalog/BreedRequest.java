package com.ppms.admin.api.catalog;

import com.ppms.admin.domain.catalog.BreedKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BreedRequest(
        @NotBlank String code,
        @NotBlank String name,
        @NotNull BreedKind kind,
        boolean active
) {
}
