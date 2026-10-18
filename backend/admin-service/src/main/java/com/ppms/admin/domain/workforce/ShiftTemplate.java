package com.ppms.admin.domain.workforce;

import com.ppms.shared.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalTime;
import java.util.UUID;

@Getter @Setter @Entity
@Table(name = "shift_template", schema = "admin")
public class ShiftTemplate extends BaseEntity {
    @Column(name = "site_id") private UUID siteId;
    @Column(nullable = false) private String code;
    @Column(name = "start_time", nullable = false) private LocalTime startTime;
    @Column(name = "end_time", nullable = false) private LocalTime endTime;
    @Column(name = "break_minutes", nullable = false) private int breakMinutes;
    private String color;
}
