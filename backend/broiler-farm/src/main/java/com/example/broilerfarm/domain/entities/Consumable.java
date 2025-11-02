package com.example.broilerfarm.domain.entities;

import com.example.broilerfarm.domain.enums.ConsumableType;
import com.example.broilerfarm.domain.enums.UnitOfMeasure;
import com.example.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "consumable")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Consumable extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    private ConsumableType type;

    @Column(name = "category")
    private String category;

    @Column(name = "unit_of_measure", nullable = false)
    @Enumerated(EnumType.STRING)
    private UnitOfMeasure unitOfMeasure;

    @Column(name = "reorder_point", nullable = false, precision = 10, scale = 2)
    private BigDecimal reorderPoint;

    @Column(name = "standard_price", precision = 10, scale = 2)
    private BigDecimal standardPrice;

    @Column(name = "supplier")
    private String supplier;

    @Column(name = "storage_requirements", length = 500)
    private String storageRequirements;

    @Column(name = "shelf_life")
    private Integer shelfLife; // in days

    // ✅ Business logic pentru validarea tipului
    public boolean isFeedType() {
        return type == ConsumableType.FEED_STARTER ||
                type == ConsumableType.FEED_GROWER ||
                type == ConsumableType.FEED_FINISHER;
    }

    public boolean isMedication() {
        return type == ConsumableType.MEDICATION ||
                type == ConsumableType.VACCINE;
    }

    public boolean requiresSpecialStorage() {
        return storageRequirements != null && !storageRequirements.isEmpty();
    }

    public boolean isPerishable() {
        return shelfLife != null && shelfLife > 0;
    }

    // ✅ Business logic pentru actualizarea prețului
    public void updateStandardPrice(BigDecimal newPrice) {
        if (newPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price must be positive");
        }
        this.standardPrice = newPrice;
    }

    public void updateReorderPoint(BigDecimal newReorderPoint) {
        if (newReorderPoint.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Reorder point cannot be negative");
        }
        this.reorderPoint = newReorderPoint;
    }
}