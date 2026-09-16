package com.example.slaughterhouse.domain.entities;


import com.example.shared.domain.entity.BaseEntity;
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
 * Represents the reception of chickens from the farm
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "chicken_receptions")
public class ChickenReception extends BaseEntity {

    @OneToOne
    @JoinColumn(name = "slaughter_lot_id", nullable = false)
    private SlaughterLot slaughterLot;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_notice_id")
    private DeliveryNotice deliveryNotice;

    @Column(name = "reception_date", nullable = false)
    private LocalDate receptionDate;

    @Column(name = "reception_time", nullable = false)
    private LocalDateTime receptionTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "received_by", nullable = false)
    private SlaughterhouseUser receivedBy;

    @Column(name = "quantity_received", nullable = false)
    private Integer quantityReceived;

    @Column(name = "chicks_alive", nullable = false)
    private Integer chicksAlive;

    @Column(name = "chicks_doa")
    private Integer chicksDOA; // Dead on Arrival

    @Column(name = "transport_conditions", length = 500)
    private String transportConditions;

    @Column(name = "animal_welfare_check", nullable = false)
    private Boolean animalWelfareCheck = false;

    @Column(name = "animal_welfare_notes", length = 1000)
    private String animalWelfareNotes;

    @Column(name = "notes", length = 1000)
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
    public Float calculateMortalityRate() {
        if (quantityReceived == 0) return 0f;
        return (float) chicksDOA / quantityReceived * 100;
    }

    public Boolean hasHighMortality() {
        return calculateMortalityRate() > 5.0f; // threshold 5%
    }
}