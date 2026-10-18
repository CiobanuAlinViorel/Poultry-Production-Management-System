package com.ppms.admin.api.catalog;

import com.ppms.admin.domain.catalog.BreedKind;

import java.util.UUID;

public record BreedDto(
        UUID id,
        String code,
        String name,
        BreedKind kind,
        boolean active
) {
}
