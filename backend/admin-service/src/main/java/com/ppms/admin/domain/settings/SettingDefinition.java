package com.ppms.admin.domain.settings;

import com.ppms.shared.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(name = "setting_definition", schema = "admin")
public class SettingDefinition extends BaseEntity {

    @Column(name = "setting_key", nullable = false, unique = true)
    private String key;

    @Column(name = "module")
    private String module;

    @Enumerated(EnumType.STRING)
    @Column(name = "setting_type", nullable = false)
    private SettingType type;

    @Column(name = "unit")
    private String unit;

    @Column(name = "default_value")
    private String defaultValue;

    @Column(name = "min_value")
    private String minValue;

    @Column(name = "max_value")
    private String maxValue;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "allowed_values", columnDefinition = "text[]")
    private String[] allowedValues;

    @Column(name = "description_ro")
    private String descriptionRo;

    @Column(name = "description_en")
    private String descriptionEn;

    @Column(name = "scopeable", nullable = false)
    private boolean scopeable = false;
}
