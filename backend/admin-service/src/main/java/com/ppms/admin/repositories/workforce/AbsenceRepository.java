package com.ppms.admin.repositories.workforce;
import com.ppms.admin.domain.workforce.Absence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface AbsenceRepository extends JpaRepository<Absence, UUID> { List<Absence> findByEmployeeId(UUID employeeId); }
