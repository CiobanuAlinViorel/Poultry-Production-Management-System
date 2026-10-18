package com.ppms.parentflock.domain;

import com.ppms.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "house", schema = "pf")
@Getter
@Setter
public class House extends BaseEntity {

    @Column(name = "site_id", nullable = false)
    private UUID siteId;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "capacity", nullable = false)
    private int capacity;

    @Column(name = "area_m2")
    private Double areaM2;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
