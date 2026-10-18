package com.ppms.reporting.controllers;

import com.ppms.reporting.domain.KpiDaily;
import com.ppms.reporting.domain.LotFact;
import com.ppms.reporting.domain.SiteOverview;
import com.ppms.reporting.services.ReportingService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reporting")
@RequiredArgsConstructor
public class ReportingController {

    private final ReportingService reportingService;

    @GetMapping("/sites")
    public ResponseEntity<List<SiteOverview>> listSites() {
        return ResponseEntity.ok(reportingService.listSites());
    }

    @GetMapping("/lots")
    public ResponseEntity<List<LotFact>> listLots() {
        return ResponseEntity.ok(reportingService.listLots());
    }

    @GetMapping("/kpi")
    public ResponseEntity<List<KpiDaily>> listKpi(
            @RequestParam UUID siteId,
            @RequestParam String metric,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(reportingService.listKpi(siteId, metric, from, to));
    }
}
