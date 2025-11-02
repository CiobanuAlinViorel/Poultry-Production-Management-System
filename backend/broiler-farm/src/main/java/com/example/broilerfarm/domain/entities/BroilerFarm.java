package com.example.broilerfarm.domain.entities;


import com.example.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

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

    @OneToOne
    @JoinColumn(name = "manager_id",unique = true, nullable = false)
    private FarmEmployee manager;

    @OneToMany(mappedBy = "broilerFarm", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<FarmEmployee> employees = new ArrayList<>();

    @OneToMany(mappedBy = "farm", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Warehouse> warehouses = new ArrayList<>();


    public void setManagerFarm(FarmEmployee manager) {
        if (this.manager != null) {
            this.manager.setBroilerFarm(null);
        }

        this.manager = manager;

        if (manager != null) {
            this.manager.setBroilerFarm(this);
        }
    }

    public void addEmployee(FarmEmployee employee) {
        employees.add(employee);
        employee.setBroilerFarm(this);
    }

    public void addWarehouse(Warehouse warehouse) {
        warehouses.add(warehouse);
        warehouse.setFarm(this);
    }

    public void removeEmployee(FarmEmployee employee) {
        employees.remove(employee);
        employee.setBroilerFarm(null);
    }

    // Constructor custom without the employees list
    public BroilerFarm(String farmName, String location, String address, Integer capacity, String licenseNumber) {
        this.farmName = farmName;
        this.location = location;
        this.address = address;
        this.capacity = capacity;
        this.licenseNumber = licenseNumber;
    }

}
