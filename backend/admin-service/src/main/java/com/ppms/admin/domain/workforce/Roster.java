package com.ppms.admin.domain.workforce;

import com.ppms.shared.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.*;
import java.util.UUID;

@Getter @Setter @Entity
@Table(name = "roster", schema = "admin")
public class Roster extends BaseEntity {
    public enum Status { DRAFT, PUBLISHED }
    @Column(name = "site_id", nullable = false) private UUID siteId;
    @Column(name = "week_start", nullable = false) private LocalDate weekStart;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Status status = Status.DRAFT;
    @Column(name = "published_by") private UUID publishedBy;
    @Column(name = "published_at") private Instant publishedAt;
}
