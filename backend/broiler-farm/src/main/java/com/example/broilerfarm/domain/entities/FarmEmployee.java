package com.example.broilerfarm.domain.entities;

import com.example.shared.domain.entity.Employee;
import com.example.shared.domain.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "farm_employees")
@Getter
@EqualsAndHashCode(callSuper = true)
@Setter
@NoArgsConstructor
@SuperBuilder
public class FarmEmployee extends Employee {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farm_id")
    private BroilerFarm broilerFarm;

    public FarmEmployee(BroilerFarm broilerFarm, String firstName, String lastName,
                        Role role, LocalDate dateOfHiring, String phone, String email) {
        // Folosește super() pentru a inițializa corect părintele
        super(firstName, lastName, email, phone, role, dateOfHiring);
        this.broilerFarm = broilerFarm;
    }

    public FarmEmployee(Long id, LocalDateTime createdAt, LocalDateTime updatedAt, String firstName, String lastName, String email, String phone, Role role, LocalDate hireDate, BroilerFarm broilerFarm) {
        super(id, createdAt, updatedAt, firstName, lastName, email, phone, role, hireDate);
        this.broilerFarm = broilerFarm;
    }
}