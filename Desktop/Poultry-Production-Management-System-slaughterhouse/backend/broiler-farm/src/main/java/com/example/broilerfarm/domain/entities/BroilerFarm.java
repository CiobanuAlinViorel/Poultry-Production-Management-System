package com.example.broilerfarm.domain.entities;


import com.example.shared.domain.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "broiler_farm")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BroilerFarm extends BaseEntity {
    @Column(name = "farm_name")
    private String farmName;

    @Column(name = "location")
    private String location;

    @Column(name="address")
    private String address;

    @Column(name = "capacity")
    private Integer capacity;

    @Column(name = "license_number" ,unique = true, nullable = false)
    private String licenseNumber;


    public BroilerFarm(Long id, LocalDateTime createdAt, LocalDateTime updatedAt, String farmName, String location, String address, Integer capacity, String licenseNumber) {
        super(id, createdAt, updatedAt);
        this.farmName = farmName;
        this.location = location;
        this.address = address;
        this.capacity = capacity;
        this.licenseNumber = licenseNumber;
    }

}
