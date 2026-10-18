package com.ppms.admin.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ppms.admin.api.settings.EffectiveSettingDto;
import com.ppms.admin.api.settings.SettingDefinitionDto;
import com.ppms.admin.api.settings.SettingUpdateRequest;
import com.ppms.admin.api.settings.SettingValueDto;
import com.ppms.admin.domain.settings.SettingDefinition;
import com.ppms.admin.domain.settings.SettingType;
import com.ppms.admin.domain.settings.SettingValue;
import com.ppms.admin.repositories.settings.SettingDefinitionRepository;
import com.ppms.admin.repositories.settings.SettingValueRepository;
import com.ppms.shared.domain.ScopeType;
import com.ppms.shared.events.DomainEvent;
import com.ppms.shared.events.OutboxPublisher;
import com.ppms.shared.events.Topics;
import com.ppms.shared.exception.BusinessRuleViolationException;
import com.ppms.shared.exception.EntityNotFoundException;
import com.ppms.shared.exception.ValidationException;
import com.ppms.shared.persistence.UuidV7Generator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SettingService {

    private final SettingDefinitionRepository definitionRepository;
    private final SettingValueRepository valueRepository;
    private final OutboxPublisher outboxPublisher;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public String getEffectiveValue(String key, ScopeType scopeType, UUID scopeId) {
        SettingDefinition definition = definitionRepository.findByKey(key)
                .orElseThrow(() -> new EntityNotFoundException("SettingDefinition", key));
        SettingValue value = resolveEffectiveValue(definition, scopeType, scopeId, Instant.now());
        return value != null ? value.getValue() : definition.getDefaultValue();
    }

    @Transactional(readOnly = true)
    public List<EffectiveSettingDto> listEffectiveValues(ScopeType scopeType, UUID scopeId) {
        Instant now = Instant.now();
        return definitionRepository.findAll().stream()
                .map(def -> {
                    SettingValue value = resolveEffectiveValue(def, scopeType, scopeId, now);
                    boolean defaulted = value == null;
                    String effectiveValue = defaulted ? def.getDefaultValue() : value.getValue();
                    return new EffectiveSettingDto(
                            def.getKey(),
                            def.getModule(),
                            effectiveValue,
                            defaulted ? ScopeType.GLOBAL : value.getScopeType(),
                            defaulted ? null : value.getScopeId(),
                            defaulted ? null : value.getEffectiveFrom(),
                            defaulted ? null : value.getChangedBy(),
                            defaulted ? null : value.getChangedAt(),
                            defaulted
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SettingDefinitionDto> listDefinitions(String module) {
        List<SettingDefinition> definitions = module != null && !module.isBlank()
                ? definitionRepository.findByModule(module)
                : definitionRepository.findAll();
        return definitions.stream().map(this::toDefinitionDto).toList();
    }

    @Transactional(readOnly = true)
    public List<SettingValueDto> history(String key, ScopeType scopeType, UUID scopeId) {
        definitionRepository.findByKey(key)
                .orElseThrow(() -> new EntityNotFoundException("SettingDefinition", key));
        return valueRepository.findHistory(key, scopeType, scopeId).stream()
                .map(this::toValueDto)
                .toList();
    }

    @Transactional
    @PreAuthorize("@scope.canManageScope(#scopeType, #scopeId)")
    public void setValue(String key, ScopeType scopeType, UUID scopeId, String value, UUID changedBy, String reason) {
        SettingDefinition definition = definitionRepository.findByKey(key)
                .orElseThrow(() -> new EntityNotFoundException("SettingDefinition", key));

        if (scopeType == ScopeType.GLOBAL) {
            scopeId = null;
        } else {
            if (!definition.isScopeable()) {
                throw new BusinessRuleViolationException("SETTING_NOT_SCOPEABLE",
                        "Setting " + key + " does not support scoped overrides");
            }
            if (scopeId == null) {
                throw new ValidationException("scopeId is required for scope type " + scopeType);
            }
        }

        validateValue(definition, value);

        Instant now = Instant.now();
        SettingValue settingValue = new SettingValue();
        settingValue.setDefinitionKey(key);
        settingValue.setScopeType(scopeType);
        settingValue.setScopeId(scopeId);
        settingValue.setValue(value);
        settingValue.setEffectiveFrom(now);
        settingValue.setChangedBy(changedBy);
        settingValue.setChangedAt(now);
        valueRepository.save(settingValue);

        publishChangedEvent(key, scopeType, scopeId, value, reason, changedBy, now);
    }

    @PreAuthorize("@scope.canManageScope(#scopeType, #scopeId)")
    public void setValue(String key, ScopeType scopeType, UUID scopeId, SettingUpdateRequest request, UUID changedBy) {
        setValue(key, scopeType, scopeId, request.value(), changedBy, request.reason());
    }

    private SettingValue resolveEffectiveValue(SettingDefinition definition, ScopeType scopeType, UUID scopeId, Instant now) {
        if (scopeType != ScopeType.GLOBAL && definition.isScopeable()) {
            Optional<SettingValue> scoped = valueRepository.findLatestEffective(definition.getKey(), scopeType, scopeId, now);
            if (scoped.isPresent()) {
                return scoped.get();
            }
        }
        if (scopeType != ScopeType.GLOBAL && !definition.isScopeable()) {
            return valueRepository.findLatestEffective(definition.getKey(), ScopeType.GLOBAL, null, now).orElse(null);
        }
        return valueRepository.findLatestEffective(definition.getKey(), scopeType, scopeId, now).orElse(null);
    }

    private void validateValue(SettingDefinition definition, String value) {
        if (value == null) {
            throw new ValidationException("Value is required");
        }
        try {
            switch (definition.getType()) {
                case INT -> {
                    int intValue = Integer.parseInt(value);
                    if (definition.getMinValue() != null && intValue < Integer.parseInt(definition.getMinValue())) {
                        throw new ValidationException("Value below minimum " + definition.getMinValue());
                    }
                    if (definition.getMaxValue() != null && intValue > Integer.parseInt(definition.getMaxValue())) {
                        throw new ValidationException("Value above maximum " + definition.getMaxValue());
                    }
                }
                case DECIMAL -> {
                    BigDecimal decimalValue = new BigDecimal(value);
                    if (definition.getMinValue() != null && decimalValue.compareTo(new BigDecimal(definition.getMinValue())) < 0) {
                        throw new ValidationException("Value below minimum " + definition.getMinValue());
                    }
                    if (definition.getMaxValue() != null && decimalValue.compareTo(new BigDecimal(definition.getMaxValue())) > 0) {
                        throw new ValidationException("Value above maximum " + definition.getMaxValue());
                    }
                }
                case BOOL -> {
                    if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
                        throw new ValidationException("Boolean value must be true or false");
                    }
                }
                case ENUM -> {
                    if (definition.getAllowedValues() != null &&
                            Arrays.stream(definition.getAllowedValues()).noneMatch(v -> v.equalsIgnoreCase(value))) {
                        throw new ValidationException("Value not in allowed values");
                    }
                }
                case JSON -> objectMapper.readTree(value);
                default -> {
                    // STRING accepts any value
                }
            }
        } catch (NumberFormatException e) {
            throw new ValidationException("Invalid numeric value: " + value);
        } catch (Exception e) {
            if (e instanceof ValidationException ve) {
                throw ve;
            }
            throw new ValidationException("Invalid value for type " + definition.getType() + ": " + value);
        }
    }

    private void publishChangedEvent(String key, ScopeType scopeType, UUID scopeId, String value,
                                     String reason, UUID changedBy, Instant changedAt) {
        UUID eventId = UuidV7Generator.generate();
        SettingChangedPayload payload = new SettingChangedPayload(key, scopeType, scopeId, value, reason, changedAt);
        DomainEvent<SettingChangedPayload> event = new DomainEvent<>(
                eventId,
                "setting.changed",
                1,
                "SETTING",
                eventId,
                scopeId,
                changedBy,
                payload
        );
        outboxPublisher.publish(Topics.ADMIN_SETTINGS, event);
    }

    private SettingDefinitionDto toDefinitionDto(SettingDefinition d) {
        return new SettingDefinitionDto(
                d.getId(), d.getKey(), d.getModule(), d.getType(), d.getUnit(),
                d.getDefaultValue(), d.getMinValue(), d.getMaxValue(),
                d.getAllowedValues(), d.getDescriptionRo(), d.getDescriptionEn(), d.isScopeable()
        );
    }

    private SettingValueDto toValueDto(SettingValue v) {
        return new SettingValueDto(
                v.getId(), v.getDefinitionKey(), v.getScopeType(), v.getScopeId(),
                v.getValue(), v.getEffectiveFrom(), v.getChangedBy(), v.getChangedAt()
        );
    }

    public record SettingChangedPayload(String key, ScopeType scopeType, UUID scopeId,
                                        String value, String reason, Instant changedAt) {
    }
}
