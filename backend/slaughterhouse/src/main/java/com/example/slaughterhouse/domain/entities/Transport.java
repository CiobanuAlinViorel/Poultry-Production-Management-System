package com.example.slaughterhouse.domain.entities;


import com.example.shared.domain.entity.BaseEntity;
import com.example.slaughterhouse.domain.enums.TransportStatus;
import com.example.slaughterhouse.domain.valueobjects.VehicleInfo;
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
import java.time.LocalTime;

/**
 * Represents transport of packages to clients
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "transports")
public class Transport extends BaseEntity {

    @OneToOne
    @JoinColumn(name = "client_delivery_notice_id", nullable = false, unique = true)
    private ClientDeliveryNotice clientDeliveryNotice;

    @Column(name = "departure_date", nullable = false)
    private LocalDate departureDate;

    @Column(name = "departure_time", nullable = false)
    private LocalTime departureTime;

    @Column(name = "arrival_date")
    private LocalDate arrivalDate;

    @Column(name = "arrival_time")
    private LocalTime arrivalTime;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "vehicleType", column = @Column(name = "vehicle_type")),
            @AttributeOverride(name = "vehiclePlate", column = @Column(name = "vehicle_plate")),
            @AttributeOverride(name = "capacity", column = @Column(name = "vehicle_capacity"))
    })
    private VehicleInfo vehicleInfo;

    @Column(name = "driver_name", length = 200)
    private String driverName;

    @Column(name = "driver_phone", length = 50)
    private String driverPhone;

    @Column(name = "packages_loaded")
    private Integer packagesLoaded;

    @Column(name = "packages_delivered")
    private Integer packagesDelivered;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "value", column = @Column(name = "total_weight_value")),
            @AttributeOverride(name = "unit", column = @Column(name = "total_weight_unit"))
    })
    private Weight totalWeight;

    @Column(name = "transport_conditions", length = 500)
    private String transportConditions;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", nullable = false, length = 50)
    private TransportStatus deliveryStatus = TransportStatus.SCHEDULED;

    @Column(name = "incident_report", length = 2000)
    private String incidentReport;

    @Column(name = "client_signature", nullable = false)
    private Boolean clientSignature = false;

    @Column(name = "delivery_receipt", length = 200)
    private String deliveryReceipt;

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
    public void startTransport() {
        this.deliveryStatus = TransportStatus.IN_TRANSIT;
    }

    public void completeDelivery(Integer delivered) {
        this.packagesDelivered = delivered;
        this.arrivalDate = LocalDate.now();
        this.arrivalTime = LocalTime.now();
        this.deliveryStatus = TransportStatus.DELIVERED;
    }

    public Integer calculateVariance() {
        if (packagesLoaded == null || packagesDelivered == null) {
            return 0;
        }
        return packagesDelivered - packagesLoaded;
    }

    public Boolean hasDiscrepancy() {
        return calculateVariance() != 0;
    }
}