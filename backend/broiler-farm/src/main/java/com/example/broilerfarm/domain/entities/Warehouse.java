package com.example.broilerfarm.domain.entities;

import com.example.shared.domain.entity.BaseEntity;
import com.example.shared.domain.entity.BaseWarehouse;
import com.example.shared.domain.enums.WarehouseType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "warehouse")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Warehouse extends BaseWarehouse {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farm_id", nullable = false)
    private BroilerFarm farm;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsible_employee_id")
    private FarmEmployee responsibleEmployee;


    public void increaseOccupancy(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        BigDecimal newOccupancy = this.getCurrentOccupancy().add(amount);
        if (newOccupancy.compareTo(this.getCapacity()) > 0) {
            throw new IllegalStateException(
                    "Warehouse capacity exceeded. Available: " + getAvailableCapacity() +
                            ", Requested: " + amount
            );
        }

        this.setCurrentOccupancy(newOccupancy);
    }

    public void decreaseOccupancy(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        BigDecimal newOccupancy = this.getCurrentOccupancy().subtract(amount);
        if (newOccupancy.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Cannot decrease occupancy below zero");
        }

        this.setCurrentOccupancy(newOccupancy);
    }


    @Transient
    public boolean isNearCapacity(BigDecimal threshold) {
        return getOccupancyPercentage().compareTo(threshold) >= 0;
    }

    public Warehouse(String warehouseName, String warehouseCode, WarehouseType type, BigDecimal capacity, BigDecimal currentOccupancy, BroilerFarm farm, FarmEmployee responsibleEmployee) {
        super(warehouseName, warehouseCode, type, capacity, currentOccupancy);
        this.farm = farm;
        this.responsibleEmployee = responsibleEmployee;
    }
}