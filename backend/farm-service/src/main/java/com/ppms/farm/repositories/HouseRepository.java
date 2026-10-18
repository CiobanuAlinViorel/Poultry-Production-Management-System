package com.ppms.farm.repositories;
import com.ppms.farm.domain.House;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface HouseRepository extends JpaRepository<House, UUID> { List<House> findAllByFarmIdOrderByCode(UUID farmId); boolean existsByFarmIdAndCodeIgnoreCase(UUID farmId, String code); }
