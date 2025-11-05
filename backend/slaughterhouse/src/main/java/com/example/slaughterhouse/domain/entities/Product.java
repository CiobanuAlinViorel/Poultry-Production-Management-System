package com.example.slaughterhouse.domain.entities;

import com.example.shared.domain.entity.BaseEntity;

import com.example.slaughterhouse.domain.enums.ProductStatus;
import com.example.slaughterhouse.domain.enums.ProductType;
import com.example.slaughterhouse.domain.valueobjects.QualityGrade;
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
 * Represents a product resulting from slaughter processing
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "products")
public class Product extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slaughter_lot_id", nullable = false)
    private SlaughterLot slaughterLot;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", nullable = false, length = 50)
    private ProductType productType;

    @Column(name = "cut", length = 100)
    private String cut; // e.g., "Breast", "Thigh", "Wing", "Whole"

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "value", column = @Column(name = "weight_value")),
            @AttributeOverride(name = "unit", column = @Column(name = "weight_unit"))
    })
    private Weight weight;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "grade", column = @Column(name = "quality_grade")),
            @AttributeOverride(name = "score", column = @Column(name = "quality_score")),
            @AttributeOverride(name = "description", column = @Column(name = "quality_description"))
    })
    private QualityGrade qualityGrade;

    @Column(name = "production_date", nullable = false)
    private LocalDate productionDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private ProductStatus status = ProductStatus.PRODUCED;

    @Column(name = "batch_number", length = 50)
    private String batchNumber;

    @Column(name = "inspection_passed", nullable = false)
    private Boolean inspectionPassed = false;

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
    public void markAsInspected(Boolean passed) {
        this.inspectionPassed = passed;
        if (passed) {
            this.status = ProductStatus.APPROVED;
        } else {
            this.status = ProductStatus.REJECTED;
        }
    }

    public void package_() {
        if (this.inspectionPassed) {
            this.status = ProductStatus.PACKAGED;
        }
    }
}