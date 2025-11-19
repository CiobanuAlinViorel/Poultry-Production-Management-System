package com.example.broilerfarm.domain.entities;

import com.example.broilerfarm.domain.enums.QualityGrade;
import com.example.shared.domain.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(name = "delivery_notice_line")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryNoticeLine extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_notice_id", nullable = false)
    private DeliveryNotice deliveryNotice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id", nullable = false)
    private ChicksLot lot;

    @Column(name = "estimated_quantity", nullable = false)
    private Integer estimatedQuantity;

    @Column(name = "average_weight", nullable = false, precision = 10, scale = 3)
    private BigDecimal averageWeight; // From ObservationSheet or manual entry

    @Column(name = "quality_grade", nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private QualityGrade qualityGrade = QualityGrade.B;

    @Column(name = "special_instructions", length = 500)
    private String specialInstructions;

    @Column(name = "loading_bay")
    private String loadingBay;

    @Column(name = "actual_quantity_delivered")
    private Integer actualQuantityDelivered; // Set after actual delivery

    @Column(name = "actual_average_weight", precision = 10, scale = 3)
    private BigDecimal actualAverageWeight; // Measured at slaughterhouse

    // ✅ Calculated field
    @Transient
    public BigDecimal getTotalEstimatedWeight() {
        return averageWeight.multiply(BigDecimal.valueOf(estimatedQuantity))
                .setScale(2, RoundingMode.HALF_UP);
    }

    // ✅ Populate from ObservationSheet (Option A - UC-10)
    public void populateFromObservationSheet(ObservationSheet observation) {
        if (observation == null) {
            throw new IllegalArgumentException("ObservationSheet cannot be null");
        }

        if (!observation.getLot().equals(this.lot)) {
            throw new IllegalStateException("ObservationSheet lot must match delivery line lot");
        }

        this.estimatedQuantity = observation.getEndingBirdCount();
        this.averageWeight = observation.getAverageWeight();

        // Determine quality grade based on performance
        if (observation.getFcr().compareTo(BigDecimal.valueOf(1.6)) <= 0) {
            this.qualityGrade = QualityGrade.A;
        } else if (observation.getFcr().compareTo(BigDecimal.valueOf(1.9)) <= 0) {
            this.qualityGrade = QualityGrade.B;
        } else {
            this.qualityGrade = QualityGrade.C;
        }
    }

    // ✅ Manual entry (Option B - UC-10)
    public void setManualData(Integer quantity, BigDecimal weight, QualityGrade grade) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        if (weight == null || weight.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Weight must be positive");
        }

        this.estimatedQuantity = quantity;
        this.averageWeight = weight;
        this.qualityGrade = grade != null ? grade : QualityGrade.B;
    }

    // ✅ Validate line data
    public void validate() {
        if (estimatedQuantity == null || estimatedQuantity <= 0) {
            throw new IllegalStateException("Estimated quantity must be positive");
        }

        if (averageWeight == null || averageWeight.compareTo(BigDecimal.valueOf(1.8)) < 0) {
            throw new IllegalStateException(
                    "Average weight must be at least 1.8 kg (current: " + averageWeight + ")"
            );
        }

        if (lot.getCurrentQuantity() < estimatedQuantity) {
            throw new IllegalStateException(
                    "Estimated quantity (" + estimatedQuantity +
                            ") exceeds available birds (" + lot.getCurrentQuantity() + ")"
            );
        }
    }

    // ✅ Partial delivery support
    @Transient
    public boolean isPartialDelivery() {
        return estimatedQuantity < lot.getCurrentQuantity();
    }

    @Transient
    public Integer getRemainingBirds() {
        return lot.getCurrentQuantity() - estimatedQuantity;
    }

    // ✅ Set actual delivery data (from slaughterhouse confirmation)
    public void setActualDeliveryData(Integer actualQuantity, BigDecimal actualWeight) {
        if (actualQuantity == null || actualQuantity <= 0) {
            throw new IllegalArgumentException("Actual quantity must be positive");
        }

        this.actualQuantityDelivered = actualQuantity;
        this.actualAverageWeight = actualWeight;
    }

    @Transient
    public boolean hasActualData() {
        return actualQuantityDelivered != null && actualAverageWeight != null;
    }

    @Transient
    public Integer getQuantityVariance() {
        if (actualQuantityDelivered == null) {
            return null;
        }
        return actualQuantityDelivered - estimatedQuantity;
    }

    @Transient
    public BigDecimal getWeightVariance() {
        if (actualAverageWeight == null) {
            return null;
        }
        return actualAverageWeight.subtract(averageWeight);
    }


    public DeliveryNoticeLine(Long id, LocalDateTime createdAt, LocalDateTime updatedAt, DeliveryNotice deliveryNotice, ChicksLot lot, Integer estimatedQuantity, BigDecimal averageWeight, QualityGrade qualityGrade, String specialInstructions, String loadingBay, Integer actualQuantityDelivered, BigDecimal actualAverageWeight) {
        super(id, createdAt, updatedAt);
        this.deliveryNotice = deliveryNotice;
        this.lot = lot;
        this.estimatedQuantity = estimatedQuantity;
        this.averageWeight = averageWeight;
        this.qualityGrade = qualityGrade;
        this.specialInstructions = specialInstructions;
        this.loadingBay = loadingBay;
        this.actualQuantityDelivered = actualQuantityDelivered;
        this.actualAverageWeight = actualAverageWeight;
    }
}