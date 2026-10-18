package com.ppms.admin.domain.settings;

import com.ppms.shared.domain.ScopeType;
import com.ppms.shared.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "setting_value", schema = "admin")
public class SettingValue extends BaseEntity {

    @Column(name = "definition_key", nullable = false)
    private String definitionKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false)
    private ScopeType scopeType;

    @Column(name = "scope_id")
    private UUID scopeId;

    @Column(name = "value", nullable = false)
    private String value;

    @Column(name = "effective_from", nullable = false)
    private Instant effectiveFrom;

    @Column(name = "changed_by")
    private UUID changedBy;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;
}
