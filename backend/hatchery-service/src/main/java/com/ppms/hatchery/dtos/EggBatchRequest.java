package com.ppms.hatchery.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record EggBatchRequest(
        @NotNull UUID hatcheryId,
        @NotBlank String batchCode,
        UUID originFlockId,
        UUID originSiteId,
        String breedCode,
        Instant collectionDate,
        Integer qtyAccepted
) {
}
