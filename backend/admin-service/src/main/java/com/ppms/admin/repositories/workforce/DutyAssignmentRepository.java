package com.ppms.admin.repositories.workforce;
import com.ppms.admin.domain.workforce.DutyAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface DutyAssignmentRepository extends JpaRepository<DutyAssignment, UUID> { List<DutyAssignment> findByRosterId(UUID rosterId); void deleteByRosterId(UUID rosterId); }
