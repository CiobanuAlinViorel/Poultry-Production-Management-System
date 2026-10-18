package com.ppms.admin.domain.workforce;

import com.ppms.shared.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Getter @Setter @Entity
@Table(name = "team", schema = "admin")
public class Team extends BaseEntity {
    @Column(name = "site_id", nullable = false) private UUID siteId;
    @Column(nullable = false) private String name;
    @Column(name = "leader_employee_id") private UUID leaderEmployeeId;
}
