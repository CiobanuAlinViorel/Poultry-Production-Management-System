package com.example.slaughterhouse.domain.entities;


import com.example.shared.domain.entity.Employee;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Concrete Employee entity for Slaughterhouse subsystem
 * Extends abstract Employee from shared-kernel
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "slaughter_employees")
public class SlaughterhouseEmployee extends Employee {

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "department", length = 100)
    private String department; // e.g., "Processing", "Quality Control", "Warehouse"

    @Column(name = "employee_code", unique = true, length = 50)
    private String employeeCode; // Internal employee code

    // Business methods
    public String getFullName() {
        return firstName + " " + lastName;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void activate() {
        this.isActive = true;
    }

}