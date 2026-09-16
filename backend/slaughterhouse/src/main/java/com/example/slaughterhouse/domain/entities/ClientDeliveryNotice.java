package com.example.slaughterhouse.domain.entities;

import com.example.shared.domain.entity.BaseEntity;

import com.example.slaughterhouse.domain.enums.ClientDeliveryStatus;
import com.example.slaughterhouse.domain.valueobjects.Address;
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
 * Represents a delivery notice for clients (outbound)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "client_delivery_notices")
public class ClientDeliveryNotice extends BaseEntity {

    @Column(name = "client_name", nullable = false, length = 200)
    private String clientName;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "street", column = @Column(name = "destination_street")),
            @AttributeOverride(name = "city", column = @Column(name = "destination_city")),
            @AttributeOverride(name = "postalCode", column = @Column(name = "destination_postal_code")),
            @AttributeOverride(name = "country", column = @Column(name = "destination_country"))
    })
    private Address destination;

    @Column(name = "scheduled_delivery_date", nullable = false)
    private LocalDate scheduledDeliveryDate;

    @Column(name = "total_packages")
    private Integer totalPackages;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "value", column = @Column(name = "total_weight_value")),
            @AttributeOverride(name = "unit", column = @Column(name = "total_weight_unit"))
    })
    private Weight totalWeight;

    @Column(name = "order_reference", length = 100)
    private String orderReference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_employee_id", nullable = false)
    private SlaughterhouseUser createdByEmployee;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private ClientDeliveryStatus status = ClientDeliveryStatus.DRAFT;

    @Column(name = "notes", length = 1000)
    private String notes;

    @ManyToMany
    @JoinTable(
            name = "client_delivery_packages",
            joinColumns = @JoinColumn(name = "delivery_notice_id"),
            inverseJoinColumns = @JoinColumn(name = "package_id")
    )
    private List<Package> packages = new ArrayList<>();

    @OneToOne(mappedBy = "clientDeliveryNotice", cascade = CascadeType.ALL, orphanRemoval = true)
    private Transport transport;

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

        Float totalWeightValue = packages.stream()
                .filter(pkg -> pkg.getWeight() != null)
                .map(pkg -> pkg.getWeight().getValue())
                .reduce(0f, Float::sum);

        if (!packages.isEmpty() && packages.get(0).getWeight() != null) {
            String unit = packages.get(0).getWeight().getUnit();
            this.totalWeight = Weight.of(totalWeightValue, unit);
        }
    }

    public void approve() {
        calculateTotals();
        this.status = ClientDeliveryStatus.APPROVED;
    }

    public void markInTransit() {
        this.status = ClientDeliveryStatus.IN_TRANSIT;
    }

    public void markDelivered() {
        this.status = ClientDeliveryStatus.DELIVERED;
    }
}