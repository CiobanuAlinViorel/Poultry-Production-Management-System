package com.ppms.slaughterhouse.domain;

import com.ppms.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "line", schema = "sl")
public class Line extends BaseEntity {

    @Column(name = "slaughterhouse_id", nullable = false)
    private UUID slaughterhouseId;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "name")
    private String name;

    @Column(name = "speed_bph")
    private Integer speedBph;

    @Column(name = "active")
    private boolean active = true;
}
