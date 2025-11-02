package com.example.broilerfarm.domain.entities;

import com.example.broilerfarm.domain.enums.WarehouseType;
import com.example.shared.domain.BaseEntity;
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
public class Warehouse extends BaseEntity {

    @Column(name = "warehouse_name", nullable = false)
    private String warehouseName;

    @Column(name = "warehouse_code", unique = true, nullable = false)
    private String warehouseCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farm_id", nullable = false)
    private BroilerFarm farm;

    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    private WarehouseType type;

    @Column(name = "capacity", nullable = false, precision = 10, scale = 2)
    private BigDecimal capacity;

    @Column(name = "current_occupancy", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal currentOccupancy = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsible_employee_id")
    private FarmEmployee responsibleEmployee;


    public void increaseOccupancy(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        BigDecimal newOccupancy = this.currentOccupancy.add(amount);
        if (newOccupancy.compareTo(this.capacity) > 0) {
            throw new IllegalStateException(
                    "Warehouse capacity exceeded. Available: " + getAvailableCapacity() +
                            ", Requested: " + amount
            );
        }

        this.currentOccupancy = newOccupancy;
    }

    public void decreaseOccupancy(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        BigDecimal newOccupancy = this.currentOccupancy.subtract(amount);
        if (newOccupancy.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Cannot decrease occupancy below zero");
        }

        this.currentOccupancy = newOccupancy;
    }


    @Transient
    public BigDecimal getAvailableCapacity() {
        return this.capacity.subtract(this.currentOccupancy);
    }

    @Transient
    public BigDecimal getOccupancyPercentage() {
        if (this.capacity.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return this.currentOccupancy
                .divide(this.capacity, 2, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    @Transient
    public boolean isFull() {
        return this.currentOccupancy.compareTo(this.capacity) >= 0;
    }

    @Transient
    public boolean isNearCapacity(BigDecimal threshold) {
        return getOccupancyPercentage().compareTo(threshold) >= 0;
    }
}