package com.ppms.farm.domain;

import com.ppms.shared.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "daily_sheet", schema = "fa", uniqueConstraints = @UniqueConstraint(columnNames = {"lot_id", "date"}))
@Getter @Setter @NoArgsConstructor
public class DailySheet extends BaseEntity {
    @Column(name = "lot_id", nullable = false) private UUID lotId;
    @Column(nullable = false) private LocalDate date;
    @Column(nullable = false) private int mortality;
    @Column(name = "feed_kg", precision = 14, scale = 3) private BigDecimal feedKg;
    @Column(name = "water_liters", precision = 14, scale = 3) private BigDecimal waterLiters;
    @Column(name = "avg_weight_g", precision = 10, scale = 2) private BigDecimal avgWeightG;
    @Column(name = "temperature_min", precision = 5, scale = 2) private BigDecimal temperatureMin;
    @Column(name = "temperature_max", precision = 5, scale = 2) private BigDecimal temperatureMax;
    @Column(name = "humidity_pct", precision = 5, scale = 2) private BigDecimal humidityPct;
    @Column(columnDefinition = "text") private String comments;
}
