package com.ppms.parentflock.dtos;

import com.ppms.parentflock.domain.Flock;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record FlockResponse(
        UUID id,
        UUID siteId,
        UUID houseId,
        String breedCode,
        LocalDate arrivalDate,
        LocalDate expectedEndDate,
        Flock.FlockStatus status,
        int femaleCount,
        int maleCount,
        Instant createdAt,
        Instant updatedAt
) {
}
