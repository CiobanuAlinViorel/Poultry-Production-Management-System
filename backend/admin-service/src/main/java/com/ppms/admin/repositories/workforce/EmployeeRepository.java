package com.ppms.admin.repositories.workforce;
import com.ppms.admin.domain.workforce.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface EmployeeRepository extends JpaRepository<Employee, UUID> { Optional<Employee> findByEmployeeNo(String employeeNo); Optional<Employee> findByUserId(UUID userId); }
