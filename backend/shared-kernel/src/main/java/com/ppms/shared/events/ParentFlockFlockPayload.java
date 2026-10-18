package com.ppms.shared.events;

import java.time.LocalDate;
import java.util.UUID;

public record ParentFlockFlockPayload(
        UUID flockId,
        UUID siteId,
        UUID houseId,
        String breedCode,
        LocalDate arrivalDate,
        LocalDate expectedEndDate,
        int femaleCount,
        int maleCount,
        String status
) {
}
