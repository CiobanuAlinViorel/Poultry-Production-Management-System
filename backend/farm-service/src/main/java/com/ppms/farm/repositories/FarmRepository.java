package com.ppms.farm.repositories;
import com.ppms.farm.domain.Farm;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface FarmRepository extends JpaRepository<Farm, UUID> { boolean existsByCodeIgnoreCase(String code); }
