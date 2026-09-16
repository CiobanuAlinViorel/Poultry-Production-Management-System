package com.example.shared.domain.repository;

import com.example.shared.domain.entity.Employee;
import com.example.shared.domain.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    // ✅ Găsește employee după email
    Optional<Employee> findByEmail(String email);

    // ✅ Găsește employees după rol
    List<Employee> findByRole(Role role);

    // ✅ Găsește employees după tip (employeeType)
    List<Employee> findByEmployeeType(String employeeType);

    // ✅ Găsește employees după rol și tip
    List<Employee> findByRoleAndEmployeeType(Role role, String employeeType);

    // ✅ Găsește employees angajați după o anumită dată
    List<Employee> findByHireDateAfter(LocalDate hireDate);

    // ✅ Găsește employees angajați înainte de o anumită dată
    List<Employee> findByHireDateBefore(LocalDate hireDate);

    // ✅ Găsește employees după nume (first name + last name)
    List<Employee> findByFirstNameAndLastName(String firstName, String lastName);

    // ✅ Găsește employees după first name (ignore case)
    List<Employee> findByFirstNameIgnoreCase(String firstName);

    // ✅ Găsește employees după last name (ignore case)
    List<Employee> findByLastNameIgnoreCase(String lastName);


    // ✅ Numără employees după rol
    long countByRole(Role role);

    // ✅ Numără employees după tip
    long countByEmployeeType(String employeeType);

    // ✅ Verifică dacă există employee cu email
    boolean existsByEmail(String email);

    // ✅ Găsește employees activi (folosind inherited field din BaseEntity)
    List<Employee> findByIsActiveTrue();

    // ✅ Găsește employees inactivi (folosind inherited field din BaseEntity)
    List<Employee> findByIsActiveFalse();



}