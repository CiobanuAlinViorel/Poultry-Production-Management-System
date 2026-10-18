package com.ppms.parentflock.dtos;

import java.time.Instant;
import java.util.UUID;

public record HouseResponse(
        UUID id,
        UUID siteId,
        String code,
        String name,
        int capacity,
        Double areaM2,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
