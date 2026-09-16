package com.example.shared.domain.entity;

import com.example.shared.domain.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;


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

    public Employee(Long id, LocalDateTime createdAt, LocalDateTime updatedAt, String firstName, String lastName, String email, String phone, Role role, LocalDate hireDate) {
        super(id, createdAt, updatedAt);
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.hireDate = hireDate;
    }
}