package com.ppms.parentflock.dtos;

import java.time.Instant;
import java.util.UUID;

public record BreederSiteResponse(
        UUID id,
        String code,
        String name,
        String address,
        String county,
        boolean active,
        int capacityHouses,
        Instant createdAt,
        Instant updatedAt
) {
}
