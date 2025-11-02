package com.example.broilerfarm.domain.entities;

import com.example.shared.domain.Employee;
import com.example.shared.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Entity
@Table(name = "farm_employees")
@PrimaryKeyJoinColumn(name = "employee_id") // ✅ JOINED inheritance
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class FarmEmployee extends Employee {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farm_id")
    private BroilerFarm broilerFarm;

    public FarmEmployee(BroilerFarm broilerFarm, String firstName, String lastName,
                        Role role, LocalDate dateOfHiring, String phone, String email) {
        super.firstName = firstName;
        super.lastName = lastName;
        super.role = role;
        super.email = email;
        super.phone = phone;
        super.hireDate = dateOfHiring;
        this.broilerFarm = broilerFarm;
        super.setEmployeeType("FARM");
    }
}