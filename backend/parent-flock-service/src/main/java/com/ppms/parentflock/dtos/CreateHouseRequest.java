package com.ppms.parentflock.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record CreateHouseRequest(
        @NotBlank String code,
        @NotBlank String name,
        @NotNull @PositiveOrZero Integer capacity,
        Double areaM2
) {
}
