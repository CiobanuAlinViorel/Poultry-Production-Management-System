package com.ppms.parentflock.repository;

import com.ppms.parentflock.domain.DailyProductionSheet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DailyProductionSheetRepository extends JpaRepository<DailyProductionSheet, UUID> {

    List<DailyProductionSheet> findByFlockIdOrderByDateDesc(UUID flockId);
}
