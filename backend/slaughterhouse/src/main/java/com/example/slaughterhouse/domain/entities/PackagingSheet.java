package com.example.slaughterhouse.domain.entities;

import com.example.shared.domain.entity.BaseEntity;
import com.example.slaughterhouse.domain.enums.PackagingStatus;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a packaging sheet for a batch of products
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "packaging_sheets")
public class PackagingSheet extends BaseEntity {

    @OneToOne
    @JoinColumn(name = "slaughter_lot_id", nullable = false)
    private SlaughterLot slaughterLot;

    @Column(name = "packaging_date", nullable = false)
    private LocalDate packagingDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "packaging_employee_id", nullable = false)
    private SlaughterhouseUser packagingEmployee;

    @Column(name = "total_products")
    private Integer totalProducts;

    @Column(name = "total_packages")
    private Integer totalPackages;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "value", column = @Column(name = "total_weight_value")),
            @AttributeOverride(name = "unit", column = @Column(name = "total_weight_unit"))
    })
    private Weight totalWeight;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private PackagingStatus status = PackagingStatus.IN_PROGRESS;

    @Column(name = "notes", length = 1000)
    private String notes;

    @OneToMany(mappedBy = "packagingSheet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Package> packages = new ArrayList<>();

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
    public void calculateTotals() {
        this.totalPackages = packages.size();
        this.totalProducts = packages.stream()
                .mapToInt(pkg -> pkg.getProducts() != null ? pkg.getProducts().size() : 0)
                .sum();

        Float totalWeightValue = packages.stream()
                .filter(pkg -> pkg.getWeight() != null)
                .map(pkg -> pkg.getWeight().getValue())
                .reduce(0f, Float::sum);

        if (!packages.isEmpty() && packages.get(0).getWeight() != null) {
            String unit = packages.get(0).getWeight().getUnit();
            this.totalWeight = Weight.of(totalWeightValue, unit);
        }
    }

    public void finalize_() {
        calculateTotals();
        this.status = PackagingStatus.COMPLETED;
    }
}