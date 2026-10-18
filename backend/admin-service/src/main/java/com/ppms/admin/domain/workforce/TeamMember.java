package com.ppms.admin.domain.workforce;

import com.ppms.shared.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.util.UUID;

@Getter @Setter @Entity
@Table(name = "team_member", schema = "admin")
public class TeamMember extends BaseEntity {
    @Column(name = "team_id", nullable = false) private UUID teamId;
    @Column(name = "employee_id", nullable = false) private UUID employeeId;
    @Column(name = "from_date", nullable = false) private LocalDate fromDate;
    @Column(name = "to_date") private LocalDate toDate;
}
