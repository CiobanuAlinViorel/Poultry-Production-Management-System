package com.ppms.slaughterhouse.domain;

import com.ppms.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "slaughterhouse", schema = "slaughterhouse")
public class Slaughterhouse extends BaseEntity {

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "name")
    private String name;

    @Column(name = "address")
    private String address;

    @Column(name = "county")
    private String county;

    @Column(name = "active")
    private boolean active = true;

    @Column(name = "daily_capacity_birds")
    private Integer dailyCapacityBirds;
}
