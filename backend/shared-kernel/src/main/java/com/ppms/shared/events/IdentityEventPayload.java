package com.ppms.shared.events;

import java.util.UUID;

public record IdentityEventPayload(
        UUID userId,
        String email,
        String username,
        String displayName,
        String token,
        String eventReason
) {
}
