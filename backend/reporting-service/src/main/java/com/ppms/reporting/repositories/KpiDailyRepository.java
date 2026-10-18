package com.ppms.reporting.repositories;

import com.ppms.reporting.domain.KpiDaily;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface KpiDailyRepository extends JpaRepository<KpiDaily, KpiDaily.KpiDailyId> {
    Optional<KpiDaily> findBySiteIdAndUnitIdAndDateAndMetric(UUID siteId, UUID unitId, LocalDate date, String metric);
}
