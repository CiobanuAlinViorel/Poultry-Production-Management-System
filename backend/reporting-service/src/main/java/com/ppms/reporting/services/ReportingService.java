package com.ppms.reporting.services;

import com.ppms.reporting.domain.KpiDaily;
import com.ppms.reporting.domain.LotFact;
import com.ppms.reporting.domain.SiteOverview;
import com.ppms.reporting.repositories.KpiDailyRepository;
import com.ppms.reporting.repositories.LotFactRepository;
import com.ppms.reporting.repositories.SiteOverviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportingService {

    private final SiteOverviewRepository siteOverviewRepository;
    private final LotFactRepository lotFactRepository;
    private final KpiDailyRepository kpiDailyRepository;

    public List<SiteOverview> listSites() {
        return siteOverviewRepository.findAll();
    }

    public List<LotFact> listLots() {
        return lotFactRepository.findAll();
    }

    public List<KpiDaily> listKpi(UUID siteId, String metric, LocalDate from, LocalDate to) {
        return kpiDailyRepository.findAll().stream()
                .filter(k -> k.getSiteId().equals(siteId))
                .filter(k -> k.getMetric().equals(metric))
                .filter(k -> !k.getDate().isBefore(from) && !k.getDate().isAfter(to))
                .toList();
    }
}
