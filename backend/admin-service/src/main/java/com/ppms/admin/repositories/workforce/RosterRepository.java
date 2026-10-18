package com.ppms.admin.repositories.workforce;
import com.ppms.admin.domain.workforce.Roster;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.*;
public interface RosterRepository extends JpaRepository<Roster, UUID> { Optional<Roster> findBySiteIdAndWeekStart(UUID siteId, LocalDate weekStart); List<Roster> findBySiteId(UUID siteId); }
