package com.example.slaughterhouse.domain.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents daily statistics for the slaughterhouse operations
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "daily_stats", uniqueConstraints = {
        @UniqueConstraint(columnNames = "record_date")
})
@EntityListeners(AuditingEntityListener.class)
public class DailyStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stats_id")
    private Long statsId;

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
    private Employee manager;

    // Audit fields
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @CreatedBy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", updatable = false)
    private Employee createdBy;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @LastModifiedBy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private Employee updatedBy;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Version
    @Column(name = "version")
    private Integer version;

    @PrePersist
    protected void onCreate() {
        if (isActive == null) {
            isActive = true;
        }
        if (recordDate == null) {
            recordDate = LocalDate.now();
        }
    }

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