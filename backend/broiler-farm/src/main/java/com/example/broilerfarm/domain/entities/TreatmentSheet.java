package com.example.broilerfarm.domain.entities;

import com.example.broilerfarm.domain.enums.TreatmentStatus;
import com.example.shared.domain.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "treatment_sheet")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TreatmentSheet extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id", nullable = false)
    private ChicksLot lot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "veterinarian_id", nullable = false)
    private FarmEmployee veterinarian;

    @Column(name = "diagnosis", nullable = false, length = 500)
    private String diagnosis;

    @Column(name = "treatment_reason", nullable = false, length = 1000)
    private String treatmentReason;

    @Column(name = "treatment_date", nullable = false)
    @Builder.Default
    private LocalDate treatmentDate = LocalDate.now();

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TreatmentStatus status = TreatmentStatus.DRAFT;

    // ✅ AGGREGATE: TreatmentSheet conține liniile sale
    @OneToMany(mappedBy = "treatmentSheet", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TreatmentSheetLine> treatmentLines = new ArrayList<>();

    // ✅ Business logic pentru gestionarea liniilor
    public void addTreatmentLine(TreatmentSheetLine line) {
        if (line == null) {
            throw new IllegalArgumentException("Treatment line cannot be null");
        }

        if (this.status != TreatmentStatus.DRAFT) {
            throw new IllegalStateException("Cannot add lines to a non-draft treatment");
        }

        treatmentLines.add(line);
        line.setTreatmentSheet(this);
    }

    public void removeTreatmentLine(TreatmentSheetLine line) {
        if (this.status != TreatmentStatus.DRAFT) {
            throw new IllegalStateException("Cannot remove lines from a non-draft treatment");
        }

        treatmentLines.remove(line);
        line.setTreatmentSheet(null);
    }

    // ✅ Activare tratament - de la DRAFT la ACTIVE
    public void activate() {
        if (this.status != TreatmentStatus.DRAFT) {
            throw new IllegalStateException("Can only activate draft treatments");
        }

        if (treatmentLines.isEmpty()) {
            throw new IllegalStateException("Cannot activate treatment without medication lines");
        }

        // Validare: toate liniile trebuie să aibă date valide
        treatmentLines.forEach(TreatmentSheetLine::validate);

        this.status = TreatmentStatus.ACTIVE;
    }

    // ✅ Completare tratament
    public void complete() {
        if (this.status != TreatmentStatus.ACTIVE) {
            throw new IllegalStateException("Can only complete active treatments");
        }

        this.status = TreatmentStatus.COMPLETED;
    }

    // ✅ Verifică dacă tratamentul este în perioadă de retragere
    @Transient
    public boolean isInWithdrawalPeriod() {
        if (this.status != TreatmentStatus.COMPLETED) {
            return true; // Tratament activ = automat în withdrawal
        }

        LocalDate today = LocalDate.now();
        return treatmentLines.stream()
                .anyMatch(line -> line.getSlaughterAllowedDate().isAfter(today));
    }

    // ✅ Data la care se poate tăia (maxim din toate liniile)
    @Transient
    public LocalDate getEarliestSlaughterDate() {
        return treatmentLines.stream()
                .map(TreatmentSheetLine::getSlaughterAllowedDate)
                .max(LocalDate::compareTo)
                .orElse(treatmentDate);
    }

    @Transient
    public boolean hasActiveWithdrawal() {
        return isInWithdrawalPeriod();
    }
}