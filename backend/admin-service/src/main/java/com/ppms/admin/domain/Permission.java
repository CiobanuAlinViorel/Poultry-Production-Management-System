package com.ppms.admin.domain;

import com.ppms.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "permission", schema = "admin")
public class Permission extends BaseEntity {

    @Column(name = "key", nullable = false, unique = true)
    private String key;

    @Column(name = "module")
    private String module;

    @Column(name = "description")
    private String description;

    @Column(name = "is_system_defined", nullable = false)
    private boolean systemDefined = true;
}
