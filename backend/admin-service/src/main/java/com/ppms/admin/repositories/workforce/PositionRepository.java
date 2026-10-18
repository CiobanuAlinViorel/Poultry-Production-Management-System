package com.ppms.admin.repositories.workforce;
import com.ppms.admin.domain.workforce.Position;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface PositionRepository extends JpaRepository<Position, UUID> { Optional<Position> findByCode(String code); }
