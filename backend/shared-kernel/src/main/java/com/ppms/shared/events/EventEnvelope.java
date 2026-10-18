package com.ppms.shared.events;

import java.time.Instant;
import java.util.UUID;

public record EventEnvelope<T>(
        UUID eventId,
        String eventType,
        int version,
        Instant occurredAt,
        String producer,
        String aggregateType,
        UUID aggregateId,
        UUID siteId,
        UUID actorId,
        UUID correlationId,
        UUID causationId,
        T payload
) {
}
