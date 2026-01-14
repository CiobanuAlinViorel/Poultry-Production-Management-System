package com.example.broilerfarm.domain.entities;

import com.example.broilerfarm.domain.enums.QualityGrade;
import com.example.broilerfarm.domain.enums.ReceptionStatus;
import com.example.shared.domain.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.cglib.core.Local;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "chicks_reception")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChicksReception extends BaseEntity {


    @Column(name = "reception_date", nullable = false)
    private LocalDateTime receptionDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farm_id", nullable = false)
    private BroilerFarm farm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiving_employee_id", nullable = false)
    private FarmEmployee receivingEmployee;

    @Column(name = "transport_conditions", length = 1000)
    private String transportConditions;

    @Column(name = "truck_info")
    private String truckInfo;

    @Column(name = "document_reference")
    private String documentReference;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ReceptionStatus status = ReceptionStatus.DRAFT;

    @Column(name = "total_quantity_received", nullable = false)
    @Builder.Default
    private Integer totalQuantityReceived = 0;

    @Column(name = "total_chicks_alive", nullable = false)
    @Builder.Default
    private Integer totalChicksAlive = 0;

    @Column(name = "total_chicks_doa", nullable = false)
    @Builder.Default
    private Integer totalChicksDOA = 0;

    @Column(name = "total_chicks_weak", nullable = false)
    @Builder.Default
    private Integer totalChicksWeak = 0;

    // ✅ AGGREGATE: ChicksReception contain its lines
    @OneToMany(mappedBy = "reception", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ChicksReceptionLine> receptionLines = new ArrayList<>();

    public ChicksReception(Long id, LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime receptionDate, BroilerFarm farm, FarmEmployee receivingEmployee, String transportConditions, String truckInfo, String documentReference, ReceptionStatus status, Integer totalQuantityReceived, Integer totalChicksAlive, Integer totalChicksDOA, Integer totalChicksWeak) {
        super(id, createdAt, updatedAt);
        this.receptionDate = receptionDate;
        this.farm = farm;
        this.receivingEmployee = receivingEmployee;
        this.transportConditions = transportConditions;
        this.truckInfo = truckInfo;
        this.documentReference = documentReference;
        this.status = status;
        this.totalQuantityReceived = totalQuantityReceived;
        this.totalChicksAlive = totalChicksAlive;
        this.totalChicksDOA = totalChicksDOA;
        this.totalChicksWeak = totalChicksWeak;
    }

    public ChicksReceptionLine getReceptionLineById(Long id){
        return this.receptionLines.stream().filter(line -> line.getId().equals(id)).findFirst().orElse(null);
    }

    // ✅ Business logic for lines management
    public void addReceptionLine(ChicksReceptionLine line) {
        if (line == null) {
            throw new IllegalArgumentException("Reception line cannot be null");
        }

        if (this.status != ReceptionStatus.DRAFT) {
            throw new IllegalStateException("Cannot add lines to a finalized reception");
        }

        receptionLines.add(line);
        line.setReception(this);


        recalculateTotals();
    }

    public void updateReceptionLine(Long id, Integer totalChicksAlive, Integer totalChicksDOA, Integer totalChicksWeak, QualityGrade qualityGrade, String notes, LocalDateTime updatedAt) {
        if(this.status != ReceptionStatus.DRAFT) {
            throw new IllegalStateException("Cannot update lines to a finalized reception");
        }
        this.verifyReceptionLineItems(  totalChicksAlive,  totalChicksDOA,  totalChicksWeak,  qualityGrade, notes, updatedAt);
        ChicksReceptionLine lineToUpdate = getReceptionLineById(id);
        if (lineToUpdate == null) {
            throw new IllegalArgumentException("Line with id " + id + " does not exist");
        }

        lineToUpdate.setChicksDOA(totalChicksDOA);
        lineToUpdate.setChicksWeak(totalChicksWeak);
        lineToUpdate.setChicksAlive(totalChicksAlive);
        lineToUpdate.setQuantity(totalChicksAlive+totalChicksDOA+totalChicksWeak);
        lineToUpdate.setQualityGrade(qualityGrade);
        lineToUpdate.setNotes(notes);

        this.recalculateTotals();

    }

    public void removeReceptionLine(Long id) {
//        if (this.status != ReceptionStatus.DRAFT) {
//            throw new IllegalStateException("Cannot remove lines from a finalized reception");
//        }

        ChicksReceptionLine lineToRemove = getReceptionLineById(id);

        receptionLines.remove(lineToRemove);
        lineToRemove.setReception(null);
        recalculateTotals();
    }

    // Recalculates the totals from lines
    public void recalculateTotals() {
        this.totalQuantityReceived = receptionLines.stream()
                .mapToInt(ChicksReceptionLine::getQuantity)
                .sum();

        this.totalChicksAlive = receptionLines.stream()
                .mapToInt(ChicksReceptionLine::getChicksAlive)
                .sum();

        this.totalChicksDOA = receptionLines.stream()
                .mapToInt(ChicksReceptionLine::getChicksDOA)
                .sum();

        this.totalChicksWeak = receptionLines.stream()
                .mapToInt(ChicksReceptionLine::getChicksWeak)
                .sum();
    }

    // ✅ Finalize reception - creates chicks lots
    public void finalizeReception() {
        if (this.status != ReceptionStatus.DRAFT) {
            throw new IllegalStateException("Reception already finalized");
        }

        if (receptionLines.isEmpty()) {
            throw new IllegalStateException("Cannot finalize reception without lines");
        }

        // Validare: toate liniile trebuie să aibă house asignat
        boolean allHousesAssigned = receptionLines.stream()
                .allMatch(line -> line.getPoultryHouse() != null);

        if (!allHousesAssigned) {
            throw new IllegalStateException("All reception lines must have a poultry house assigned");
        }

        this.status = ReceptionStatus.CONFIRMED;
    }

    private void verifyReceptionLineItems( Integer totalChicksAlive, Integer totalChicksDOA, Integer totalChicksWeak, QualityGrade qualityGrade, String notes, LocalDateTime updatedAt){
        if(totalChicksAlive <= 0){
            throw new IllegalArgumentException("Total chicks alive must be positive");
        }
        if(totalChicksDOA < 0){
            throw new IllegalArgumentException("Total chicks DOA must be positive or 0");
        }
        if(totalChicksWeak < 0){
            throw new IllegalArgumentException("Total chicks Weak must be positive or 0");
        }
        if(qualityGrade == null){
            throw new IllegalArgumentException("Quality grade cannot be null");
        }
        if(notes == null){
            throw new IllegalArgumentException("Notes cannot be null");
        }
    }

    // ✅ Validates totals
    @Transient
    public boolean areTotalsValid() {
        return totalQuantityReceived == (totalChicksAlive + totalChicksDOA + totalChicksWeak);
    }

    @Transient
    public double getMortalityRate() {
        if (totalQuantityReceived == 0) {
            return 0.0;
        }
        return (totalChicksDOA * 100.0) / totalQuantityReceived;
    }

    @Transient
    public double getWeakChicksRate() {
        if (totalQuantityReceived == 0) {
            return 0.0;
        }
        return (totalChicksWeak * 100.0) / totalQuantityReceived;
    }

    @Transient
    public boolean isHighMortality() {
        return getMortalityRate() > 2.0; // > 2% DOA is concerning
    }
}