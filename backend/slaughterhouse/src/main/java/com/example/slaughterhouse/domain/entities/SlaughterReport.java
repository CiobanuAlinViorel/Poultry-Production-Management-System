package com.example.slaughterhouse.domain.entities;

import com.example.slaughterhouse.domain.valueobjects.DateRange;
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
 * Represents a slaughter report for a lot
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "slaughter_reports")
@EntityListeners(AuditingEntityListener.class)
public class SlaughterReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long reportId;

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
        if (reportDate == null) {
            reportDate = LocalDate.now();
        }
    }

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