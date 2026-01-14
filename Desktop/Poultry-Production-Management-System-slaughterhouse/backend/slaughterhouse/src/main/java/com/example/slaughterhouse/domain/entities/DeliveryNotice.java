package com.example.slaughterhouse.domain.entities;

import com.example.shared.domain.entity.BaseEntity;
import com.example.slaughterhouse.domain.enums.ReceptionStatus;
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
 * Represents a delivery notice received from Farm Management System
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "delivery_notices")
public class DeliveryNotice extends BaseEntity {

    @Column(name = "external_system_id", nullable = false, unique = true, length = 100)
    private String externalSystemId; // ID from Farm Management System

    @Column(name = "source_system", nullable = false, length = 100)
    private String sourceSystem = "FARM_MANAGEMENT";

    @Column(name = "farm_origin", length = 200)
    private String farmOrigin;

    @Column(name = "lot_number_from_farm", length = 100)
    private String lotNumberFromFarm;

    @Column(name = "scheduled_delivery_date", nullable = false)
    private LocalDate scheduledDeliveryDate;

    @Column(name = "estimated_quantity", nullable = false)
    private Integer estimatedQuantity;

    @Column(name = "average_weight")
    private Float averageWeight;

    @Column(name = "breed", length = 100)
    private String breed;

    @Column(name = "average_age_in_days")
    private Integer averageAgeInDays;

    @Column(name = "transport_details", length = 500)
    private String transportDetails;

    @Column(name = "vehicle_plate", length = 50)
    private String vehiclePlate;

    @Column(name = "driver_info", length = 200)
    private String driverInfo;

    @Column(name = "received_date")
    private LocalDate receivedDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "received_by")
    private SlaughterhouseUser receivedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "reception_status", nullable = false, length = 50)
    private ReceptionStatus receptionStatus = ReceptionStatus.PENDING;

    @Column(name = "notes", length = 1000)
    private String notes;

    @OneToOne(mappedBy = "deliveryNotice", cascade = CascadeType.ALL, orphanRemoval = true)
    private ChickenReception chickenReception;

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
    public Boolean isReceived() {
        return receptionStatus == ReceptionStatus.RECEIVED;
    }

    public void markAsReceived(SlaughterhouseUser employee) {
        this.receivedDate = LocalDate.now();
        this.receivedBy = employee;
        this.receptionStatus = ReceptionStatus.RECEIVED;
    }

    public void confirmReception() {
        this.receptionStatus = ReceptionStatus.CONFIRMED;
    }
}