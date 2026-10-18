package com.ppms.admin.repositories.workforce;
import com.ppms.admin.domain.workforce.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface TeamRepository extends JpaRepository<Team, UUID> { List<Team> findBySiteId(UUID siteId); }
