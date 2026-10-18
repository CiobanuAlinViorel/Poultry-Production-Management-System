package com.ppms.shared.events;

import java.time.Instant;
import java.util.UUID;

public record SiteDirectoryPayload(
        UUID siteId,
        String siteType,
        String code,
        String name,
        String address,
        String county,
        boolean active,
        String sourceService,
        Instant lastEventAt
) {
}
