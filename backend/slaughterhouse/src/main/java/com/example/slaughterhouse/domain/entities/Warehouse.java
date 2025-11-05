package com.example.slaughterhouse.domain.entities;

import com.example.shared.domain.entity.BaseWarehouse;
import com.example.slaughterhouse.domain.valueobjects.Address;
import com.example.slaughterhouse.domain.valueobjects.Temperature;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a warehouse for cold storage
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "warehouses")
public class Warehouse extends BaseWarehouse {

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


    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "value", column = @Column(name = "temperature_value")),
            @AttributeOverride(name = "unit", column = @Column(name = "temperature_unit"))
    })
    private Temperature temperature;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private SlaughterhouseUser manager;


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
    private SlaughterhouseUser createdBy;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @LastModifiedBy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private SlaughterhouseUser updatedBy;

    @Version
    @Column(name = "version")
    private Integer version;


    public Boolean isOverCapacity() {
        return currentOccupancy.compareTo(capacity) > 0;
    }

    public void updateOccupancy(BigDecimal delta) {
        this.currentOccupancy =  this.currentOccupancy.add(delta);
        if (this.currentOccupancy.compareTo(BigDecimal.ZERO) > 0) {
            this.currentOccupancy = this.currentOccupancy.add(BigDecimal.ZERO);
        }
    }
}