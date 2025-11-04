package com.example.shared.domain.entity;

import com.example.shared.domain.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;



@Getter
@Setter
@NoArgsConstructor
@MappedSuperclass
@AllArgsConstructor
@SuperBuilder
public abstract class Employee extends BaseEntity {

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

}