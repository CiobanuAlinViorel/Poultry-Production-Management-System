package com.ppms.slaughterhouse.domain;

import com.ppms.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "slaughter_session", schema = "slaughterhouse")
public class SlaughterSession extends BaseEntity {

    @Column(name = "slaughterhouse_id", nullable = false)
    private UUID slaughterhouseId;

    @Column(name = "line_id")
    private UUID lineId;

    @Column(name = "session_code", nullable = false, unique = true)
    private String sessionCode;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "birds_planned")
    private Integer birdsPlanned;

    @Column(name = "birds_processed")
    private Integer birdsProcessed;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status = Status.PLANNED;

    public enum Status { PLANNED, IN_PROGRESS, COMPLETED, CANCELLED }
}
