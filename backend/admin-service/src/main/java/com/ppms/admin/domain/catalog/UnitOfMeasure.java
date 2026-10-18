package com.ppms.admin.domain.catalog;

import com.ppms.shared.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "unit_of_measure", schema = "admin")
public class UnitOfMeasure extends BaseEntity {

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "name")
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false)
    private UnitOfMeasureKind kind;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
