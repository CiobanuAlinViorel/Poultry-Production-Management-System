package com.example.broilerfarm.domain.entities;

import com.example.broilerfarm.domain.enums.StockStatus;
import com.example.shared.domain.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "consumable_stock")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsumableStock extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consumable_id", nullable = false)
    private Consumable consumable;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(name = "batch_number", nullable = false)
    private String batchNumber;

    @Column(name = "quantity_on_hand", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal quantityOnHand = BigDecimal.ZERO;

    @Column(name = "reserved_quantity", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal reservedQuantity = BigDecimal.ZERO;

    @Column(name = "last_restock_date")
    private LocalDate lastRestockDate;

    @Column(name = "manufacturing_date")
    private LocalDate manufacturingDate;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private StockStatus status = StockStatus.AVAILABLE;

    // ✅ Calculated field
    @Transient
    public BigDecimal getAvailableQuantity() {
        return quantityOnHand.subtract(reservedQuantity);
    }

    // ✅ Business logic for available stock
    public void addStock(BigDecimal quantity) {
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        this.quantityOnHand = this.quantityOnHand.add(quantity);
        this.lastRestockDate = LocalDate.now();
        updateStatus();
    }

    public void consumeStock(BigDecimal quantity) {
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        BigDecimal availableQty = getAvailableQuantity();
        if (quantity.compareTo(availableQty) > 0) {
            throw new IllegalStateException(
                    "Insufficient stock. Available: " + availableQty +
                            ", Requested: " + quantity
            );
        }

        this.quantityOnHand = this.quantityOnHand.subtract(quantity);
        updateStatus();
    }

    public void reserveStock(BigDecimal quantity) {
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        BigDecimal availableQty = getAvailableQuantity();
        if (quantity.compareTo(availableQty) > 0) {
            throw new IllegalStateException(
                    "Cannot reserve more than available. Available: " + availableQty +
                            ", Requested: " + quantity
            );
        }

        this.reservedQuantity = this.reservedQuantity.add(quantity);
        updateStatus();
    }

    public void releaseReservation(BigDecimal quantity) {
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        if (quantity.compareTo(this.reservedQuantity) > 0) {
            throw new IllegalStateException(
                    "Cannot release more than reserved. Reserved: " + this.reservedQuantity +
                            ", Requested: " + quantity
            );
        }

        this.reservedQuantity = this.reservedQuantity.subtract(quantity);
        updateStatus();
    }

    // ✅ Automated update status
    private void updateStatus() {
        if (this.quantityOnHand.compareTo(BigDecimal.ZERO) == 0) {
            this.status = StockStatus.DEPLETED;
        } else if (isExpired()) {
            this.status = StockStatus.EXPIRED;
        } else if (this.reservedQuantity.compareTo(BigDecimal.ZERO) > 0) {
            this.status = StockStatus.RESERVED;
        } else {
            this.status = StockStatus.AVAILABLE;
        }
    }


    @Transient
    public boolean isExpired() {
        return expirationDate != null && LocalDate.now().isAfter(expirationDate);
    }

    @Transient
    public boolean isExpiringSoon(int daysThreshold) {
        if (expirationDate == null) {
            return false;
        }
        LocalDate thresholdDate = LocalDate.now().plusDays(daysThreshold);
        return expirationDate.isBefore(thresholdDate) || expirationDate.isEqual(thresholdDate);
    }

    @Transient
    public long getDaysUntilExpiration() {
        if (expirationDate == null) {
            return Long.MAX_VALUE;
        }
        return LocalDate.now().until(expirationDate, java.time.temporal.ChronoUnit.DAYS);
    }

    @Transient
    public boolean isBelowReorderPoint(BigDecimal reorderPoint) {
        return getAvailableQuantity().compareTo(reorderPoint) < 0;
    }
}