package com.example.slaughterhouse.domain.entities;

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

/**
 * Represents the reception of chickens from the farm
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "chicken_receptions")
@EntityListeners(AuditingEntityListener.class)
public class ChickenReception {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reception_id")
    private Long receptionId;

    @OneToOne
    @JoinColumn(name = "slaughter_lot_id", nullable = false)
    private SlaughterLot slaughterLot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_notice_id")
    private DeliveryNotice deliveryNotice;

    @Column(name = "reception_date", nullable = false)
    private LocalDate receptionDate;

    @Column(name = "reception_time", nullable = false)
    private LocalDateTime receptionTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "received_by", nullable = false)
    private Employee receivedBy;

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
        if (receptionTime == null) {
            receptionTime = LocalDateTime.now();
        }
        if (animalWelfareCheck == null) {
            animalWelfareCheck = false;
        }
    }

    // Business methods
    public Float calculateMortalityRate() {
        if (quantityReceived == 0) return 0f;
        return (float) chicksDOA / quantityReceived * 100;
    }

    public Boolean hasHighMortality() {
        return calculateMortalityRate() > 5.0f; // threshold 5%
    }
}