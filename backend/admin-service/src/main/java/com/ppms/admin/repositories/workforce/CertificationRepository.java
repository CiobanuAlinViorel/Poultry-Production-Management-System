package com.ppms.admin.repositories.workforce;
import com.ppms.admin.domain.workforce.Certification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface CertificationRepository extends JpaRepository<Certification, UUID> { List<Certification> findByEmployeeId(UUID employeeId); }
