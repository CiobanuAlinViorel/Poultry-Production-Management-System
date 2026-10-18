package com.ppms.admin.api.catalog;

import com.ppms.admin.domain.catalog.UnitOfMeasureKind;

import java.util.UUID;

public record UnitOfMeasureDto(
        UUID id,
        String code,
        String name,
        UnitOfMeasureKind kind,
        boolean active
) {
}
