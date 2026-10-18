package com.ppms.admin.api.settings;

import com.ppms.shared.domain.ScopeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SettingUpdateRequest(
        @NotNull ScopeType scopeType,
        UUID scopeId,
        @NotBlank String value,
        @NotBlank String reason
) {
}
