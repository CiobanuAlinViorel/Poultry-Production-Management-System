package com.ppms.parentflock.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;
import java.util.UUID;

public record CreateEggBatchRequest(
        @NotNull UUID flockId,
        @NotBlank String batchCode,
        @NotNull LocalDate laidDate,
        @NotNull @PositiveOrZero Integer storageDays,
        String qualityGrade,
        @NotNull @PositiveOrZero Integer quantity,
        String reservedFor
) {
}
