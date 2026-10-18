package com.ppms.parentflock.dtos;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record DailyProductionSheetResponse(
        UUID id,
        UUID flockId,
        LocalDate date,
        int eggsTotal,
        int eggsCracked,
        int eggsDirty,
        int eggsSmall,
        int eggsDoubleYolk,
        Double feedKg,
        Double waterLiters,
        int mortality,
        Instant createdAt,
        Instant updatedAt
) {
}
