package com.ppms.shared.events;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record FarmDailySheetPayload(
        UUID lotId,
        UUID farmId,
        UUID houseId,
        LocalDate date,
        int mortality,
        BigDecimal feedKg,
        BigDecimal waterLiters,
        BigDecimal avgWeightG,
        BigDecimal temperatureMin,
        BigDecimal temperatureMax,
        BigDecimal humidityPct
) {
}
