package com.ppms.shared.events;

import java.util.UUID;

public record DomainEvent<T>(
        UUID eventId,
        String eventType,
        int version,
        String aggregateType,
        UUID aggregateId,
        UUID siteId,
        UUID actorId,
        T payload
) {
}
