package com.ppms.hatchery.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record IncubatorRequest(
        @NotNull UUID hatcheryId,
        @NotBlank String code,
        String type,
        String manufacturer,
        String model,
        Integer capacityEggs,
        String status
) {
}
