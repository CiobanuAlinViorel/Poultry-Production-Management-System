package com.ppms.parentflock.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;

public record CreateDailySheetRequest(
        @NotNull LocalDate date,
        @NotNull @PositiveOrZero Integer eggsTotal,
        @NotNull @PositiveOrZero Integer eggsCracked,
        @NotNull @PositiveOrZero Integer eggsDirty,
        @NotNull @PositiveOrZero Integer eggsSmall,
        @NotNull @PositiveOrZero Integer eggsDoubleYolk,
        Double feedKg,
        Double waterLiters,
        @NotNull @PositiveOrZero Integer mortality
) {
}
