package com.ppms.reporting.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "kpi_daily", schema = "reporting")
@IdClass(KpiDaily.KpiDailyId.class)
public class KpiDaily {

    @Id
    @Column(name = "site_id")
    private UUID siteId;

    @Id
    @Column(name = "unit_id")
    private UUID unitId;

    @Id
    @Column(name = "date")
    private LocalDate date;

    @Id
    @Column(name = "metric")
    private String metric;

    @Column(name = "value")
    private Double value;

    @Getter
    @Setter
    public static class KpiDailyId implements Serializable {
        private UUID siteId;
        private UUID unitId;
        private LocalDate date;
        private String metric;
    }
}
