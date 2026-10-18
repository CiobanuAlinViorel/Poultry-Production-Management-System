package com.ppms.admin.domain.workforce;

import com.ppms.shared.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.util.UUID;

@Getter @Setter @Entity
@Table(name = "absence", schema = "admin")
public class Absence extends BaseEntity {
    public enum Type { VACATION, SICK, UNPAID, OTHER }
    @Column(name = "employee_id", nullable = false) private UUID employeeId;
    @Column(name = "from_date", nullable = false) private LocalDate fromDate;
    @Column(name = "to_date", nullable = false) private LocalDate toDate;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Type type;
    @Column(name = "approved_by") private UUID approvedBy;
}
