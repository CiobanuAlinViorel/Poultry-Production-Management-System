package com.example.slaughterhouse.domain.entities;

import com.example.shared.domain.entity.BaseEntity;
import com.example.slaughterhouse.domain.valueobjects.DateRange;
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
 * Represents a slaughter report for a lot
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "slaughter_reports")
public class SlaughterReport extends BaseEntity {

    @OneToOne
    @JoinColumn(name = "slaughter_lot_id", nullable = false, unique = true)
    private SlaughterLot slaughterLot;

    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;

    @Column(name = "total_received", nullable = false)
    private Integer totalReceived;

    @Column(name = "total_processed", nullable = false)
    private Integer totalProcessed;

    @Column(name = "approved")
    private Integer approved;

    @Column(name = "rejected")
    private Integer rejected;

    @Column(name = "total_yield")
    private Float totalYield; // in kg

    @Column(name = "yield_percentage")
    private Float yieldPercentage;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "startDate", column = @Column(name = "period_start_date")),
            @AttributeOverride(name = "endDate", column = @Column(name = "period_end_date"))
    })
    private DateRange reportPeriod;

    @Column(name = "notes", length = 2000)
    private String notes;

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
        if (totalReceived == 0) {
            this.yieldPercentage = 0f;
            return;
        }

        if (totalYield != null) {
            // Assuming average chicken weight for calculation
            Float estimatedTotalWeight = totalReceived * 2.0f; // 2kg average
            this.yieldPercentage = (totalYield / estimatedTotalWeight) * 100;
        }
    }

    public Float calculateProcessingRate() {
        if (totalReceived == 0) return 0f;
        return (float) totalProcessed / totalReceived * 100;
    }

    public Float calculateApprovalRate() {
        if (totalProcessed == 0) return 0f;
        return (float) approved / totalProcessed * 100;
    }

    public Float calculateRejectionRate() {
        if (totalProcessed == 0) return 0f;
        return (float) rejected / totalProcessed * 100;
    }
}