package com.example.slaughterhouse.domain.entities;


import com.example.shared.domain.entity.BaseEntity;
import com.example.slaughterhouse.domain.enums.DisposalMethod;
import com.example.slaughterhouse.domain.enums.WasteCategory;
import com.example.slaughterhouse.domain.valueobjects.DateRange;
import com.example.slaughterhouse.domain.valueobjects.Weight;
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
 * Represents a waste report for a slaughter lot
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "waste_reports")
public class WasteReport extends BaseEntity {

    @OneToOne
    @JoinColumn(name = "slaughter_lot_id", nullable = false, unique = true)
    private SlaughterLot slaughterLot;

    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "value", column = @Column(name = "total_waste_weight_value")),
            @AttributeOverride(name = "unit", column = @Column(name = "total_waste_weight_unit"))
    })
    private Weight totalWasteWeight;

    @Column(name = "waste_type", length = 200)
    private String wasteType; // e.g., "Feathers", "Viscera", "Blood", "Rejected carcasses"

    @Enumerated(EnumType.STRING)
    @Column(name = "waste_category", length = 50)
    private WasteCategory wasteCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "disposal_method", nullable = false, length = 50)
    private DisposalMethod disposalMethod;

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
    public Float calculateWastePercentage(Float totalProcessedWeight) {
        if (totalProcessedWeight == 0 || totalWasteWeight == null) {
            return 0f;
        }
        return (totalWasteWeight.getValue() / totalProcessedWeight) * 100;
    }

    public Boolean isHighWaste(Float threshold) {
        if (totalWasteWeight == null) return false;
        return totalWasteWeight.getValue() > threshold;
    }
}