package com.example.slaughterhouse.domain.entities;

import com.example.slaughterhouse.domain.enums.WarehouseType;
import com.example.slaughterhouse.domain.valueobjects.Address;
import com.example.slaughterhouse.domain.valueobjects.Temperature;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a warehouse for cold storage
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "warehouses")
@EntityListeners(AuditingEntityListener.class)
public class Warehouse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(name = "warehouse_name", nullable = false, length = 200)
    private String warehouseName;

    @Column(name = "warehouse_code", nullable = false, unique = true, length = 50)
    private String warehouseCode;

    @Column(name = "location", length = 200)
    private String location;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "street", column = @Column(name = "address_street")),
            @AttributeOverride(name = "city", column = @Column(name = "address_city")),
            @AttributeOverride(name = "postalCode", column = @Column(name = "address_postal_code")),
            @AttributeOverride(name = "country", column = @Column(name = "address_country"))
    })
    private Address address;

    @Column(name = "total_capacity", nullable = false)
    private Float totalCapacity; // in cubic meters or pallets

    @Column(name = "current_occupancy", nullable = false)
    private Float currentOccupancy = 0f;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "value", column = @Column(name = "temperature_value")),
            @AttributeOverride(name = "unit", column = @Column(name = "temperature_unit"))
    })
    private Temperature temperature;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private Employee manager;

    @Enumerated(EnumType.STRING)
    @Column(name = "warehouse_type", nullable = false, length = 50)
    private WarehouseType warehouseType;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @OneToMany(mappedBy = "warehouse", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ColdStorageLog> storageLogs = new ArrayList<>();

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

    @Version
    @Column(name = "version")
    private Integer version;

    @PrePersist
    protected void onCreate() {
        if (isActive == null) {
            isActive = true;
        }
        if (currentOccupancy == null) {
            currentOccupancy = 0f;
        }
    }

    // Business methods
    public Float getAvailableSpace() {
        return totalCapacity - currentOccupancy;
    }

    public Float getOccupancyPercentage() {
        if (totalCapacity == 0) return 0f;
        return (currentOccupancy / totalCapacity) * 100;
    }

    public Boolean hasCapacity(Float requiredSpace) {
        return getAvailableSpace() >= requiredSpace;
    }

    public Boolean isOverCapacity() {
        return currentOccupancy > totalCapacity;
    }

    public void updateOccupancy(Float delta) {
        this.currentOccupancy += delta;
        if (this.currentOccupancy < 0) {
            this.currentOccupancy = 0f;
        }
    }
}