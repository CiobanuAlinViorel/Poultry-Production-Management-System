package com.example.slaughterhouse.domain.entities;

import com.example.shared.domain.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents daily statistics for the slaughterhouse operations
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "daily_stats", uniqueConstraints = {
        @UniqueConstraint(columnNames = "record_date")
})
public class DailyStats extends BaseEntity {

    @Column(name = "record_date", nullable = false, unique = true)
    private LocalDate recordDate;

    @Column(name = "total_chickens_received")
    private Integer totalChickensReceived;

    @Column(name = "total_processed")
    private Integer totalProcessed;

    @Column(name = "total_approved")
    private Integer totalApproved;

    @Column(name = "total_rejected")
    private Integer totalRejected;

    @Column(name = "total_yield")
    private Float totalYield; // in kg

    @Column(name = "yield_percentage")
    private Float yieldPercentage;

    @Column(name = "average_weight")
    private Float averageWeight;

    @Column(name = "total_lots_processed")
    private Integer totalLotsProcessed;

    @Column(name = "total_packages_produced")
    private Integer totalPackagesProduced;

    @Column(name = "total_deliveries")
    private Integer totalDeliveries;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private SlaughterhouseUser manager;

    // Audit fields
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @CreatedBy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", updatable = false)
    private SlaughterhouseUser createdBy;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @LastModifiedBy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private SlaughterhouseUser updatedBy;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Version
    @Column(name = "version")
    private Integer version;


    // Business methods
    public void calculateYieldPercentage() {
        if (totalChickensReceived == 0) {
            this.yieldPercentage = 0f;
            return;
        }

        if (totalYield != null && averageWeight != null) {
            Float estimatedTotalWeight = totalChickensReceived * averageWeight;
            this.yieldPercentage = (totalYield / estimatedTotalWeight) * 100;
        }
    }

    public Float calculateProcessingEfficiency() {
        if (totalChickensReceived == 0) return 0f;
        return (float) totalProcessed / totalChickensReceived * 100;
    }

    public Float calculateApprovalRate() {
        if (totalProcessed == 0) return 0f;
        return (float) totalApproved / totalProcessed * 100;
    }

    public Float calculateRejectionRate() {
        if (totalProcessed == 0) return 0f;
        return (float) totalRejected / totalProcessed * 100;
    }

    public Float calculateAveragePackagesPerLot() {
        if (totalLotsProcessed == 0) return 0f;
        return (float) totalPackagesProduced / totalLotsProcessed;
    }
}