package com.example.shared.domain;

import com.example.shared.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Entity
@Table(name = "employees")
@Inheritance(strategy = InheritanceType.JOINED) // ✅ JOINED strategy
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Employee extends BaseEntity {

    @Column(name = "first_name", nullable = false)
    protected String firstName;

    @Column(name = "last_name", nullable = false)
    protected String lastName;

    @Column(name = "email", unique = true, nullable = false)
    protected String email;

    @Column(name = "phone")
    protected String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    protected Role role;

    @Column(name = "hire_date", nullable = false)
    protected LocalDate hireDate;

    // ✅ Discriminator pentru a identifica tipul de employee
    @Column(name = "employee_type")
    private String employeeType; // "FARM", "HATCHERY", "SLAUGHTERHOUSE"
}