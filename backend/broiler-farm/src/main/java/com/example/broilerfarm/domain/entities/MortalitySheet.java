package com.example.broilerfarm.domain.entities;

import com.example.broilerfarm.domain.enums.DisposalMethod;
import com.example.broilerfarm.domain.enums.MortalitySheetStatus;
import com.example.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Entity
@Table(name = "mortality_sheet")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MortalitySheet extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id", nullable = false)
    private ChicksLot lot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporting_employee_id", nullable = false)
    private FarmEmployee reportingEmployee;

    @Column(name = "sheet_date", nullable = false)
    @Builder.Default
    private LocalDate sheetDate = LocalDate.now();

    @Column(name = "total_mortality", nullable = false)
    @Builder.Default
    private Integer totalMortality = 0;

    @Column(name = "cumulative_mortality", nullable = false)
    @Builder.Default
    private Integer cumulativeMortality = 0;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private MortalitySheetStatus status = MortalitySheetStatus.DRAFT;

    @Column(name = "primary_cause", length = 500)
    private String primaryCause;

    @Column(name = "age_in_days", nullable = false)
    private Integer ageInDays;

    @Column(name = "average_weight", precision = 10, scale = 3)
    private BigDecimal averageWeight;

    @Column(name = "disposal_method", nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private DisposalMethod disposalMethod = DisposalMethod.BURIAL;

    @Column(name = "location_notes", length = 500)
    private String locationNotes;

    @Column(name = "observations", length = 1000)
    private String observations;

    // ✅ Calculated fields pentru rate-uri
    @Transient
    public BigDecimal getDailyMortalityRate() {
        if (lot == null || lot.getCurrentQuantity() == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(totalMortality)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(lot.getCurrentQuantity()), 4, RoundingMode.HALF_UP);
    }

    @Transient
    public BigDecimal getCumulativeMortalityRate() {
        if (lot == null || lot.getInitialQuantity() == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(cumulativeMortality)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(lot.getInitialQuantity()), 4, RoundingMode.HALF_UP);
    }

    // ✅ Business logic pentru înregistrare mortalitate
    public void recordMortality(Integer deathCount, String cause) {
        if (this.status != MortalitySheetStatus.DRAFT) {
            throw new IllegalStateException("Cannot modify approved mortality sheet");
        }

        if (deathCount == null || deathCount <= 0) {
            throw new IllegalArgumentException("Death count must be positive");
        }

        this.totalMortality += deathCount;

        if (cause != null && !cause.trim().isEmpty()) {
            this.primaryCause = (this.primaryCause == null ? "" : this.primaryCause + "; ") + cause;
        }
    }

    // ✅ Finalizare și trimitere la aprobare
    public void submit() {
        if (this.status != MortalitySheetStatus.DRAFT) {
            throw new IllegalStateException("Can only submit draft sheets");
        }

        if (this.totalMortality < 0) {
            throw new IllegalStateException("Total mortality cannot be negative");
        }

        // Calculează age in days automat
        if (this.lot != null && this.ageInDays == null) {
            this.ageInDays = this.lot.getDaysInFarm();
        }

        this.status = MortalitySheetStatus.SUBMITTED;
    }

    // ✅ Aprobare de către Farm Manager
    public void approve(FarmEmployee approver, Integer previousCumulativeMortality) {
        if (this.status != MortalitySheetStatus.SUBMITTED) {
            throw new IllegalStateException("Can only approve submitted sheets");
        }

        // Calculează cumulative mortality
        this.cumulativeMortality = previousCumulativeMortality + this.totalMortality;

        this.status = MortalitySheetStatus.APPROVED;

        // ✅ IMPORTANT: Actualizarea lotului se face în Domain Service, NU aici!
    }

    // ✅ Respingere
    public void reject(String reason) {
        if (this.status != MortalitySheetStatus.SUBMITTED) {
            throw new IllegalStateException("Can only reject submitted sheets");
        }

        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Rejection reason is required");
        }

        this.status = MortalitySheetStatus.REJECTED;
        this.observations = (this.observations == null ? "" : this.observations + " | ")
                + "REJECTED: " + reason;
    }

    // ✅ Verificări pentru alerte
    @Transient
    public boolean isHighMortality(BigDecimal threshold) {
        return getDailyMortalityRate().compareTo(threshold) > 0;
    }

    @Transient
    public boolean isCumulativeHighMortality(BigDecimal expectedRate) {
        BigDecimal alertThreshold = expectedRate.add(BigDecimal.valueOf(2.0)); // Expected + 2%
        return getCumulativeMortalityRate().compareTo(alertThreshold) > 0;
    }

    @Transient
    public boolean requiresInvestigation() {
        // Mortalitate zilnică > 0.5% SAU cumulativ > expected + 2%
        return isHighMortality(BigDecimal.valueOf(0.5)) ||
                (lot != null && isCumulativeHighMortality(BigDecimal.valueOf(lot.getExpectedMortalityRate())));
    }
}