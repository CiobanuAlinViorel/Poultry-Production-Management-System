package com.ppms.reporting.repositories;

import com.ppms.reporting.domain.KpiDaily;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KpiDailyRepository extends JpaRepository<KpiDaily, KpiDaily.KpiDailyId> {
}
