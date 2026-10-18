package com.ppms.admin.domain.workforce;

import com.ppms.shared.domain.ScopeType;
import com.ppms.shared.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "employee", schema = "admin")
public class Employee extends BaseEntity {
    public enum Status { ACTIVE, ON_LEAVE, TERMINATED }
    @Column(name = "employee_no", nullable = false, unique = true) private String employeeNo;
    @Column(name = "first_name", nullable = false) private String firstName;
    @Column(name = "last_name", nullable = false) private String lastName;
    @Column(name = "cnp_hash", length = 64) private String cnpHash;
    private String phone;
    private String email;
    @Column(name = "hire_date", nullable = false) private LocalDate hireDate;
    @Column(name = "termination_date") private LocalDate terminationDate;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Status status = Status.ACTIVE;
    @Column(name = "position_id") private UUID positionId;
    @Column(name = "home_site_id") private UUID homeSiteId;
    @Enumerated(EnumType.STRING) @Column(name = "home_site_type") private ScopeType homeSiteType;
    @Column(name = "user_id") private UUID userId;
    @Column(name = "photo_url") private String photoUrl;
}
