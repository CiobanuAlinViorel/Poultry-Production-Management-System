package com.example.broilerfarm.domain.entities;

import com.example.broilerfarm.domain.enums.AdministrationMethod;
import com.example.shared.domain.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "treatment_sheet_line")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TreatmentSheetLine extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "treatment_sheet_id", nullable = false)
    private TreatmentSheet treatmentSheet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medication_id", nullable = false)
    private Consumable medication;

    @Column(name = "dosage", nullable = false, precision = 10, scale = 3)
    private BigDecimal dosage;

    @Column(name = "dosage_unit", nullable = false)
    private String dosageUnit; // ex: "mg/kg", "ml/L", "g/bird"

    @Column(name = "administration_method", nullable = false)
    @Enumerated(EnumType.STRING)
    private AdministrationMethod administrationMethod;

    @Column(name = "duration", nullable = false)
    private Integer duration; // days

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "withdrawal_period", nullable = false)
    private Integer withdrawalPeriod; // days

    @Column(name = "quantity_used", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal quantityUsed = BigDecimal.ZERO;

    @Column(name = "batch_number")
    private String batchNumber;

    // ✅ Calculated fields
    @Transient
    public LocalDate getSlaughterAllowedDate() {
        return endDate.plusDays(withdrawalPeriod);
    }

    // ✅ Business logic pentru validare
    public void validate() {
        if (dosage.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Dosage must be positive");
        }

        if (duration <= 0) {
            throw new IllegalArgumentException("Duration must be positive");
        }

        if (withdrawalPeriod < 0) {
            throw new IllegalArgumentException("Withdrawal period cannot be negative");
        }

        if (endDate.isBefore(startDate)) {
            throw new IllegalStateException("End date cannot be before start date");
        }

        // Verifică că medicamentul este de tip MEDICATION sau VACCINE
        if (medication != null && !medication.isMedication()) {
            throw new IllegalStateException(
                    "Only MEDICATION or VACCINE consumables can be used in treatment"
            );
        }
    }

    // ✅ Calcul automat end date
    public void calculateEndDate() {
        if (startDate != null && duration != null) {
            this.endDate = startDate.plusDays(duration - 1); // -1 pentru că include startDate
        }
    }

    // ✅ Verifică dacă tratamentul este încă activ
    @Transient
    public boolean isActive() {
        LocalDate today = LocalDate.now();
        return !today.isBefore(startDate) && !today.isAfter(endDate);
    }

    @Transient
    public boolean isCompleted() {
        return LocalDate.now().isAfter(endDate);
    }

    @Transient
    public boolean isInWithdrawal() {
        LocalDate today = LocalDate.now();
        return isCompleted() && today.isBefore(getSlaughterAllowedDate());
    }

    @Transient
    public int getDaysRemainingInWithdrawal() {
        if (!isInWithdrawal()) {
            return 0;
        }
        LocalDate today = LocalDate.now();
        return (int) today.until(getSlaughterAllowedDate(), java.time.temporal.ChronoUnit.DAYS);
    }

    public TreatmentSheetLine(Long id, LocalDateTime createdAt, LocalDateTime updatedAt, TreatmentSheet treatmentSheet, Consumable medication, BigDecimal dosage, String dosageUnit, AdministrationMethod administrationMethod, Integer duration, LocalDate startDate, LocalDate endDate, Integer withdrawalPeriod, BigDecimal quantityUsed, String batchNumber) {
        super(id, createdAt, updatedAt);
        this.treatmentSheet = treatmentSheet;
        this.medication = medication;
        this.dosage = dosage;
        this.dosageUnit = dosageUnit;
        this.administrationMethod = administrationMethod;
        this.duration = duration;
        this.startDate = startDate;
        this.endDate = endDate;
        this.withdrawalPeriod = withdrawalPeriod;
        this.quantityUsed = quantityUsed;
        this.batchNumber = batchNumber;
    }
}