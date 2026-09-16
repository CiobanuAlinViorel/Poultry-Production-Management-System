package com.example.shared.domain.entity;

import com.example.shared.domain.enums.WarehouseType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@MappedSuperclass
public abstract class BaseWarehouse extends BaseEntity {
    @Column(name = "warehouse_name", nullable = false)
    protected String warehouseName;

    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    protected WarehouseType type;

    @Column(name = "capacity", nullable = false, precision = 10, scale = 2)
    protected BigDecimal capacity;

    @Column(name = "current_occupancy", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    protected BigDecimal currentOccupancy = BigDecimal.ZERO;

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

    public BaseWarehouse(Long id, LocalDateTime createdAt, LocalDateTime updatedAt, String warehouseName, WarehouseType type, BigDecimal capacity, BigDecimal currentOccupancy) {
        super(id, createdAt, updatedAt);
        this.warehouseName = warehouseName;
        this.type = type;
        this.capacity = capacity;
        this.currentOccupancy = currentOccupancy;
    }
}
