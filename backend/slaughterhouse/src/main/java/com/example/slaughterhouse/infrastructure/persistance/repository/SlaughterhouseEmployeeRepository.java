package com.example.slaughterhouse.infrastructure.persistance.repository;

import com.example.slaughterhouse.domain.entities.SlaughterhouseEmployee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SlaughterhouseEmployeeRepository extends JpaRepository<SlaughterhouseEmployee, Long> {

    // Căutare după cod intern de angajat
    Optional<SlaughterhouseEmployee> findByEmployeeCode(String employeeCode);

    // Căutare după nume complet
    Optional<SlaughterhouseEmployee> findByFirstNameAndLastName(String firstName, String lastName);
}
