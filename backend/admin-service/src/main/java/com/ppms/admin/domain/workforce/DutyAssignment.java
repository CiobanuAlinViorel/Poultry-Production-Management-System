package com.ppms.admin.domain.workforce;

import com.ppms.shared.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.util.UUID;

@Getter @Setter @Entity
@Table(name = "duty_assignment", schema = "admin")
public class DutyAssignment extends BaseEntity {
    @Column(name = "roster_id", nullable = false) private UUID rosterId;
    @Column(name = "employee_id", nullable = false) private UUID employeeId;
    @Column(nullable = false) private LocalDate date;
    @Column(name = "shift_template_id", nullable = false) private UUID shiftTemplateId;
    @Column(name = "sub_unit_id") private UUID subUnitId;
    @Column(name = "role_hint") private String roleHint;
    private String note;
}
