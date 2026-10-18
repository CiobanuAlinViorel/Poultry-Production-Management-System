package com.ppms.reporting.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "lot_fact", schema = "reporting")
public class LotFact {

    @Id
    @Column(name = "lot_id")
    private UUID lotId;

    @Column(name = "lot_code")
    private String lotCode;

    @Column(name = "farm_id")
    private UUID farmId;

    @Column(name = "farm_name")
    private String farmName;

    @Column(name = "house_id")
    private UUID houseId;

    @Column(name = "breed_code")
    private String breedCode;

    @Column(name = "placement_date")
    private Instant placementDate;

    @Column(name = "close_date")
    private Instant closeDate;

    @Column(name = "initial_birds")
    private Integer initialBirds;

    @Column(name = "current_birds")
    private Integer currentBirds;

    @Column(name = "mortality_pct")
    private Double mortalityPct;

    @Column(name = "avg_weight_g")
    private Double avgWeightG;

    @Column(name = "fcr")
    private Double fcr;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
