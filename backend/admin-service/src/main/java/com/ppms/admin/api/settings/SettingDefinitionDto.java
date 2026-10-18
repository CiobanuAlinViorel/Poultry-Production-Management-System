package com.ppms.admin.api.settings;

import com.ppms.admin.domain.settings.SettingType;

import java.util.UUID;

public record SettingDefinitionDto(
        UUID id,
        String key,
        String module,
        SettingType type,
        String unit,
        String defaultValue,
        String minValue,
        String maxValue,
        String[] allowedValues,
        String descriptionRo,
        String descriptionEn,
        boolean scopeable
) {
}
