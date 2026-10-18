package com.ppms.admin.api.settings;

import com.ppms.shared.domain.ScopeType;

import java.time.Instant;
import java.util.UUID;

public record SettingValueDto(
        UUID id,
        String definitionKey,
        ScopeType scopeType,
        UUID scopeId,
        String value,
        Instant effectiveFrom,
        UUID changedBy,
        Instant changedAt
) {
}
