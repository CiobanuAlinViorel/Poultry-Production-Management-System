package com.ppms.parentflock.domain;

import com.ppms.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "daily_production_sheet", schema = "pf")
@Getter
@Setter
public class DailyProductionSheet extends BaseEntity {

    @Column(name = "flock_id", nullable = false)
    private UUID flockId;

    @Column(name = "sheet_date", nullable = false)
    private LocalDate date;

    @Column(name = "eggs_total", nullable = false)
    private int eggsTotal;

    @Column(name = "eggs_cracked", nullable = false)
    private int eggsCracked;

    @Column(name = "eggs_dirty", nullable = false)
    private int eggsDirty;

    @Column(name = "eggs_small", nullable = false)
    private int eggsSmall;

    @Column(name = "eggs_double_yolk", nullable = false)
    private int eggsDoubleYolk;

    @Column(name = "feed_kg")
    private Double feedKg;

    @Column(name = "water_liters")
    private Double waterLiters;

    @Column(name = "mortality", nullable = false)
    private int mortality;
}
