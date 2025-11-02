package com.example.broilerfarm.domain.entities;

import com.example.broilerfarm.domain.enums.QualityGrade;
import com.example.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "chicks_reception_line")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChicksReceptionLine extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reception_id", nullable = false)
    private ChicksReception reception;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poultry_house_id", nullable = false)
    private PoultryHouse poultryHouse;

    // ✅ Critical relationship - a line creates a chicks lot
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "created_lot_id", unique = true)
    private ChicksLot createdLot;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "chicks_alive", nullable = false)
    private Integer chicksAlive;

    @Column(name = "chicks_doa", nullable = false)
    @Builder.Default
    private Integer chicksDOA = 0;

    @Column(name = "chicks_weak", nullable = false)
    @Builder.Default
    private Integer chicksWeak = 0;

    @Column(name = "quality_grade", nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private QualityGrade qualityGrade = QualityGrade.B;

    @Column(name = "notes", length = 1000)
    private String notes;

    // ✅ Business logic for validation
    public void validateQuantities() {
        if (quantity != (chicksAlive + chicksDOA + chicksWeak)) {
            throw new IllegalStateException(
                    "Total quantity must equal alive + DOA + weak. " +
                            "Expected: " + quantity + ", Got: " + (chicksAlive + chicksDOA + chicksWeak)
            );
        }
    }

    // ✅ Create lot for this line
    public ChicksLot createLotForHouse(String breed, String hatcherySource) {
        validateQuantities();

        if (this.poultryHouse == null) {
            throw new IllegalStateException("Cannot create lot without assigned poultry house");
        }

        if (this.createdLot != null) {
            throw new IllegalStateException("Lot already created for this reception line");
        }

        // Generates lot number: FARM-HOUSE-DATE
        String lotNumber = String.format("%d-%d-%s",
                this.poultryHouse.getFarm().getId(),
                this.poultryHouse.getId(),
                this.reception.getReceptionDate().toLocalDate().toString()
        );

        ChicksLot lot = ChicksLot.builder()
                .lotNumber(lotNumber)
                .house(this.poultryHouse)
                .breed(breed)
                .hatcherySource(hatcherySource)
                .receptionDate(this.reception.getReceptionDate().toLocalDate())
                .initialQuantity(this.chicksAlive)
                .currentQuantity(this.chicksAlive)
                .build();

        this.createdLot = lot;
        return lot;
    }

    @Transient
    public boolean isLotCreated() {
        return this.createdLot != null;
    }

    @Transient
    public double getLineDoaRate() {
        if (quantity == 0) {
            return 0.0;
        }
        return (chicksDOA * 100.0) / quantity;
    }
}