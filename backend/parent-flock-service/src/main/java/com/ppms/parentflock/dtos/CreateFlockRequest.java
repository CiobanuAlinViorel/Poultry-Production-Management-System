package com.ppms.parentflock.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;
import java.util.UUID;

public record CreateFlockRequest(
        @NotNull UUID houseId,
        @NotBlank String breedCode,
        @NotNull LocalDate arrivalDate,
        LocalDate expectedEndDate,
        @NotNull @PositiveOrZero Integer femaleCount,
        @NotNull @PositiveOrZero Integer maleCount
) {
}
