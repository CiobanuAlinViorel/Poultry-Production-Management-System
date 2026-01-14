package com.example.slaughterhouse.domain.entities;

import com.example.shared.domain.entity.BaseEntity;
import com.example.slaughterhouse.domain.enums.LotStatus;
import com.example.slaughterhouse.domain.valueobjects.HealthStatus;
import com.example.slaughterhouse.domain.valueobjects.QualityGrade;
import com.example.slaughterhouse.domain.valueobjects.Weight;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a lot of chickens received for slaughter processing
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "slaughter_lots")
@EntityListeners(AuditingEntityListener.class)

public class SlaughterLot extends BaseEntity {


    @Column(name = "lot_number", nullable = false, unique = true, length = 50)
    private String lotNumber;

    @Column(name = "breed", length = 100)
    private String breed;

    @Column(name = "slaughter_date")
    private LocalDate slaughterDate;

    @Column(name = "total_chickens", nullable = false)
    private Integer totalChickens;

    @Column(name = "current_quantity", nullable = false)
    private Integer currentQuantity;

    @Column(name = "average_age_in_days")
    private Integer averageAgeInDays;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private LotStatus status = LotStatus.RECEIVED;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "value", column = @Column(name = "average_weight_value")),
            @AttributeOverride(name = "unit", column = @Column(name = "average_weight_unit"))
    })
    private Weight averageWeight;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "value", column = @Column(name = "total_weight_value")),
            @AttributeOverride(name = "unit", column = @Column(name = "total_weight_unit"))
    })
    private Weight totalWeight;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "status", column = @Column(name = "health_status")),
            @AttributeOverride(name = "description", column = @Column(name = "health_description")),
            @AttributeOverride(name = "severity", column = @Column(name = "health_severity"))
    })
    private HealthStatus healthStatus;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "grade", column = @Column(name = "quality_grade")),
            @AttributeOverride(name = "score", column = @Column(name = "quality_score")),
            @AttributeOverride(name = "description", column = @Column(name = "quality_description"))
    })
    private QualityGrade qualityGrade;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private SlaughterhouseEmployee manager;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @OneToOne(mappedBy = "slaughterLot", cascade = CascadeType.ALL, orphanRemoval = true)
    private ChickenReception chickenReception;

    @OneToOne(mappedBy = "slaughterLot", cascade = CascadeType.ALL, orphanRemoval = true)
    private AnteMortemInspection anteMortemInspection;

    @OneToMany(mappedBy = "slaughterLot", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Product> products = new ArrayList<>();

    @OneToOne(mappedBy = "slaughterLot", cascade = CascadeType.ALL, orphanRemoval = true)
    private PackagingSheet packagingSheet;

    @OneToOne(mappedBy = "slaughterLot", cascade = CascadeType.ALL, orphanRemoval = true)
    private SlaughterReport slaughterReport;

    @OneToOne(mappedBy = "slaughterLot", cascade = CascadeType.ALL, orphanRemoval = true)
    private WasteReport wasteReport;

    // Business methods (no need for audit fields - inherited from BaseEntity!)
    public void updateCurrentQuantity(Integer quantity) {
        this.currentQuantity = quantity;
    }

    public void calculateTotalWeight() {
        if (averageWeight != null && averageWeight.getValue() != null &&
                currentQuantity != null && averageWeight.getUnit() != null) {
            Float totalValue = averageWeight.getValue() * currentQuantity;
            this.totalWeight = Weight.of(totalValue, averageWeight.getUnit());
        }
    }

    public Integer calculateMortality() {
        return totalChickens - currentQuantity;
    }

    public Float calculateMortalityRate() {
        if (totalChickens == 0) return 0f;
        return (float) calculateMortality() / totalChickens * 100;
    }

}