package com.example.broilerfarm.domain.entities;

import com.example.broilerfarm.domain.enums.QualityGrade;
import com.example.shared.domain.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Cascade;

import java.time.LocalDateTime;

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
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_number", unique = true)
    @Cascade({org.hibernate.annotations.CascadeType.PERSIST,
            org.hibernate.annotations.CascadeType.MERGE})
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
    @Column(name = "breed", length = 1000)
    private String breed;

    @Column(name = "hatchery_source", length = 1000)
    private String hatcherySource;

    // ✅ Business logic for validation
    public void validateQuantities() {
        if (quantity != (chicksAlive + chicksDOA + chicksWeak)) {
            throw new IllegalStateException(
                    "Total quantity must equal alive + DOA + weak. " +
                            "Expected: " + quantity + ", Got: " + (chicksAlive + chicksDOA + chicksWeak)
            );
        }
    }

    @Transient
    public double getLineDoaRate() {
        if (quantity == 0) {
            return 0.0;
        }
        return (chicksDOA * 100.0) / quantity;
    }

    public ChicksReceptionLine(Long id, LocalDateTime createdAt, LocalDateTime updatedAt, ChicksReception reception, PoultryHouse poultryHouse, ChicksLot createdLot, Integer quantity, Integer chicksAlive, Integer chicksDOA, Integer chicksWeak, QualityGrade qualityGrade, String notes) {
        super(id, createdAt, updatedAt);
        this.reception = reception;
        this.poultryHouse = poultryHouse;
        this.createdLot = createdLot;
        this.quantity = quantity;
        this.chicksAlive = chicksAlive;
        this.chicksDOA = chicksDOA;
        this.chicksWeak = chicksWeak;
        this.qualityGrade = qualityGrade;
        this.notes = notes;
    }
}