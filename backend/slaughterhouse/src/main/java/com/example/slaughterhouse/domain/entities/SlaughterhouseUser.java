package com.example.slaughterhouse.domain.entities;

import com.example.shared.domain.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Concrete User entity for Slaughterhouse subsystem
 * Extends abstract User from shared-kernel
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity

public class SlaughterhouseUser extends User {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private SlaughterhouseEmployee employee; // Link to employee

    // Additional slaughterhouse-specific fields if needed
    @Column(name = "access_level")
    private Integer accessLevel; // Custom access level for slaughterhouse operations

    @Column(name = "notes", length = 1000)
    private String notes;
}