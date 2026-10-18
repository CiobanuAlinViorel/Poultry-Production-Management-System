package com.ppms.farm.repositories;
import com.ppms.farm.domain.Silo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface SiloRepository extends JpaRepository<Silo, UUID> { List<Silo> findAllByFarmIdOrderByCode(UUID farmId); boolean existsByFarmIdAndCodeIgnoreCase(UUID farmId, String code); }
