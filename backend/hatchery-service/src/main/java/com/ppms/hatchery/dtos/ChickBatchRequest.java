package com.ppms.hatchery.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record ChickBatchRequest(
        @NotNull UUID hatcheryId,
        @NotBlank String batchCode,
        UUID hatchRecordId,
        Instant hatchDate,
        String grade,
        Integer qtyAvailable
) {
}
