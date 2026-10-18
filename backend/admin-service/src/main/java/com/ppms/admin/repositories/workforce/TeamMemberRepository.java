package com.ppms.admin.repositories.workforce;
import com.ppms.admin.domain.workforce.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface TeamMemberRepository extends JpaRepository<TeamMember, UUID> { List<TeamMember> findByTeamId(UUID teamId); Optional<TeamMember> findFirstByTeamIdAndEmployeeIdAndToDateIsNull(UUID teamId, UUID employeeId); }
