package com.ppms.admin.repositories.workforce;
import com.ppms.admin.domain.workforce.ShiftTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface ShiftTemplateRepository extends JpaRepository<ShiftTemplate, UUID> { List<ShiftTemplate> findBySiteId(UUID siteId); }
