package com.example.slaughterhouse.domain.entities;

import com.example.shared.domain.entity.BaseEntity;
import com.example.slaughterhouse.domain.enums.ApprovalStatus;
import com.example.slaughterhouse.domain.valueobjects.HealthStatus;
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
 * Represents ante-mortem inspection performed before slaughter
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "antemortem_inspections")
public class AnteMortemInspection extends BaseEntity {

    @OneToOne
    @JoinColumn(name = "slaughter_lot_id", nullable = false)
    private SlaughterLot slaughterLot;

    @Column(name = "inspection_date", nullable = false)
    private LocalDate inspectionDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "veterinarian_id", nullable = false)
    private SlaughterhouseUser veterinarian;

    @Column(name = "total_inspected", nullable = false)
    private Integer totalInspected;

    @Column(name = "approved")
    private Integer approved;

    @Column(name = "rejected")
    private Integer rejected;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "status", column = @Column(name = "health_status")),
            @AttributeOverride(name = "description", column = @Column(name = "health_description")),
            @AttributeOverride(name = "severity", column = @Column(name = "health_severity"))
    })
    private HealthStatus healthStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 50)
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;

    @Column(name = "rejection_reasons", length = 1000)
    private String rejectionReasons;

    @Column(name = "notes", length = 1000)
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
    public Float calculateApprovalRate() {
        if (totalInspected == 0) return 0f;
        return (float) approved / totalInspected * 100;
    }

    public Float calculateRejectionRate() {
        if (totalInspected == 0) return 0f;
        return (float) rejected / totalInspected * 100;
    }

    public void approve() {
        this.approvalStatus = ApprovalStatus.APPROVED;
    }

    public void reject(String reasons) {
        this.approvalStatus = ApprovalStatus.REJECTED;
        this.rejectionReasons = reasons;
    }
}