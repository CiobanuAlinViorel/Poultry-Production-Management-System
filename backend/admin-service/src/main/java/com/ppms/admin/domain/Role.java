package com.ppms.admin.domain;

import com.ppms.shared.domain.ScopeType;
import com.ppms.shared.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "role", schema = "admin")
public class Role extends BaseEntity {

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "name_ro")
    private String nameRo;

    @Column(name = "name_en")
    private String nameEn;

    @Column(name = "description")
    private String description;

    @Column(name = "module")
    private String module;

    @ElementCollection
    @CollectionTable(name = "role_allowed_scope_types", schema = "admin", joinColumns = @JoinColumn(name = "role_id"))
    @Column(name = "scope_type")
    @Enumerated(EnumType.STRING)
    private Set<ScopeType> allowedScopeTypes = new HashSet<>();

    @Column(name = "is_system_defined", nullable = false)
    private boolean systemDefined = true;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
