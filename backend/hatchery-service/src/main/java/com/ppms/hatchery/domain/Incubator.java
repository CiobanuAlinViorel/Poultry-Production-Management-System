package com.ppms.hatchery.domain;

import com.ppms.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "incubator", schema = "ha")
public class Incubator extends BaseEntity {

    @Column(name = "hatchery_id", nullable = false)
    private java.util.UUID hatcheryId;

    @Column(name = "code", nullable = false)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private Type type;

    @Column(name = "manufacturer")
    private String manufacturer;

    @Column(name = "model")
    private String model;

    @Column(name = "capacity_eggs")
    private Integer capacityEggs;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private Status status = Status.AVAILABLE;

    public enum Type { SETTER, HATCHER, COMBI }
    public enum Status { AVAILABLE, IN_USE, CLEANING, MAINTENANCE, OUT_OF_SERVICE }
}
