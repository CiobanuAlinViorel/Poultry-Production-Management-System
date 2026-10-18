package com.ppms.admin.domain.workforce;

import com.ppms.shared.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "position", schema = "admin")
public class Position extends BaseEntity {
    @Column(nullable = false, unique = true) private String code;
    @Column(name = "name_ro", nullable = false) private String nameRo;
    @Column(name = "name_en") private String nameEn;
    private String module;
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "default_role_ids", columnDefinition = "uuid[]")
    private UUID[] defaultRoleIds = new UUID[0];
}
