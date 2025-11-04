package com.example.slaughterhouse.domain.entities;

import com.example.slaughterhouse.domain.enums.PackageStatus;
import com.example.slaughterhouse.domain.enums.PackageType;
import com.example.slaughterhouse.domain.valueobjects.PackageCode;
import com.example.slaughterhouse.domain.valueobjects.Weight;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a package containing products
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "packages")
@EntityListeners(AuditingEntityListener.class)
public class Package {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "package_id")
    private Long packageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "packaging_sheet_id", nullable = false)
    private PackagingSheet packagingSheet;

    @ManyToMany
    @JoinTable(
            name = "package_products",
            joinColumns = @JoinColumn(name = "package_id"),
            inverseJoinColumns = @JoinColumn(name = "product_id")
    )
    private List<Product> products = new ArrayList<>();

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "prefix", column = @Column(name = "package_code_prefix")),
            @AttributeOverride(name = "number", column = @Column(name = "package_code_number")),
            @AttributeOverride(name = "suffix", column = @Column(name = "package_code_suffix"))
    })
    private PackageCode packageCode;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "value", column = @Column(name = "weight_value")),
            @AttributeOverride(name = "unit", column = @Column(name = "weight_unit"))
    })
    private Weight weight;

    @Column(name = "packaging_date", nullable = false)
    private LocalDate packagingDate;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "package_type", nullable = false, length = 50)
    private PackageType packageType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private PackageStatus status = PackageStatus.PACKAGED;

    @OneToOne(mappedBy = "package_", cascade = CascadeType.ALL, orphanRemoval = true)
    private ColdStorageLog coldStorageLog;

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
        if (status == null) {
            status = PackageStatus.PACKAGED;
        }
        if (packagingDate == null) {
            packagingDate = LocalDate.now();
        }
        if (expiryDate == null) {
            // Default expiry: 30 days from packaging
            expiryDate = packagingDate.plusDays(30);
        }
    }

    // Business methods
    public Boolean isExpired() {
        return LocalDate.now().isAfter(expiryDate);
    }

    public Boolean isExpiringSoon() {
        return LocalDate.now().plusDays(7).isAfter(expiryDate);
    }

    public void moveToStorage() {
        this.status = PackageStatus.IN_STORAGE;
    }

    public void markForDelivery() {
        this.status = PackageStatus.READY_FOR_DELIVERY;
    }
}