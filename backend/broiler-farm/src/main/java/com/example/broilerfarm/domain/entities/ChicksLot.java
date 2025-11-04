package com.example.broilerfarm.domain.entities;

import com.example.broilerfarm.domain.enums.ChicksLotStatus;
import com.example.shared.domain.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "chicks_lot")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChicksLot extends BaseEntity {

    @Column(name = "lot_number", unique = true, nullable = false)
    private String lotNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "house_id", nullable = false)
    private PoultryHouse house;

    @Column(name = "hatchery_source")
    private String hatcherySource;

    @Column(name = "breed")
    private String breed;

    @Column(name = "reception_date", nullable = false)
    private LocalDate receptionDate;

    @Column(name = "initial_quantity", nullable = false)
    private int initialQuantity;

    @Column(name = "current_quantity", nullable = false)
    private int currentQuantity;

    @Column(name = "expected_slaughter_date")
    private LocalDate expectedSlaughterDate;

    @Column(name = "actual_slaughter_date")
    private LocalDate actualSlaughterDate;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ChicksLotStatus status = ChicksLotStatus.GROWING;

    @Column(name = "expected_mortality_rate")
    @Builder.Default
    private double expectedMortalityRate = 0.0;


    public void updateCurrentQuantity(int newQuantity) {
        if (newQuantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }
        this.currentQuantity = newQuantity;
    }

    public void markAsReadyForDelivery() {
        if (this.status != ChicksLotStatus.GROWING) {
            throw new IllegalStateException("Can only mark GROWING lots as ready");
        }
        this.status = ChicksLotStatus.READY_FOR_DELIVERY;
    }

    public void markAsDelivered(LocalDate deliveryDate) {
        this.status = ChicksLotStatus.DELIVERED;
        this.actualSlaughterDate = deliveryDate;
    }


    @Transient
    public int getDaysInFarm() {
        return (int) ChronoUnit.DAYS.between(this.receptionDate, LocalDate.now());
    }
}