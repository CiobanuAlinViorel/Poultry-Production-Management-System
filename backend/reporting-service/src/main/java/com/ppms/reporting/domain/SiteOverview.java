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
@Table(name = "site_overview", schema = "reporting")
public class SiteOverview {

    @Id
    @Column(name = "site_id")
    private UUID siteId;

    @Column(name = "site_type")
    private String siteType;

    @Column(name = "name")
    private String name;

    @Column(name = "active_units")
    private Integer activeUnits;

    @Column(name = "birds")
    private Integer birds;

    @Column(name = "open_critical_alerts")
    private Integer openCriticalAlerts;

    @Column(name = "open_warning_alerts")
    private Integer openWarningAlerts;

    @Column(name = "last_activity_at")
    private Instant lastActivityAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
