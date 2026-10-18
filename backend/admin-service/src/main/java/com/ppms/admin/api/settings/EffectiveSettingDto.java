package com.ppms.admin.api.settings;

import com.ppms.shared.domain.ScopeType;

import java.time.Instant;
import java.util.UUID;

public record EffectiveSettingDto(
        String key,
        String module,
        String value,
        ScopeType scopeType,
        UUID scopeId,
        Instant effectiveFrom,
        UUID changedBy,
        Instant changedAt,
        boolean defaulted
) {
}
