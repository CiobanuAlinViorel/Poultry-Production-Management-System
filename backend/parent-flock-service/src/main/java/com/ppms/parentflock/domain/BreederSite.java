package com.ppms.parentflock.domain;

import com.ppms.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "breeder_site", schema = "pf")
@Getter
@Setter
public class BreederSite extends BaseEntity {

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "county", length = 100)
    private String county;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "capacity_houses", nullable = false)
    private int capacityHouses;
}
