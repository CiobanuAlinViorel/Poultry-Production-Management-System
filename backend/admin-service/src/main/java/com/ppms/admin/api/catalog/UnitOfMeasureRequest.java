package com.ppms.admin.api.catalog;

import com.ppms.admin.domain.catalog.UnitOfMeasureKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UnitOfMeasureRequest(
        @NotBlank String code,
        @NotBlank String name,
        @NotNull UnitOfMeasureKind kind,
        boolean active
) {
}
