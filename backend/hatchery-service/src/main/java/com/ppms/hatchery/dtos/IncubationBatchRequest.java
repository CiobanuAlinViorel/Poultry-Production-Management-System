package com.ppms.hatchery.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record IncubationBatchRequest(
        @NotNull UUID hatcheryId,
        @NotBlank String batchCode,
        UUID setterId,
        Instant setAt,
        Instant expectedHatchDate,
        Integer qtySet
) {
}
