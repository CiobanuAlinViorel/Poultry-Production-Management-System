package com.example.broilerfarm.services;

import com.example.broilerfarm.domain.entities.ChicksLot;
import com.example.broilerfarm.domain.entities.ObservationSheet;
import com.example.broilerfarm.domain.enums.ObservationSheetStatus;
import com.example.broilerfarm.infrastructure.persistence.repositories.ChicksLotRepository;
import com.example.broilerfarm.infrastructure.persistence.repositories.ObservationSheetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Domain Service pentru calculul performanței loturilor
 * UC-03: Monitoring the Chick Stock for All Farm
 */
@Service
@RequiredArgsConstructor
public class LotPerformanceCalculatorService {

    private final ChicksLotRepository chicksLotRepository;
    private final ObservationSheetRepository observationSheetRepository;

    /**
     * Calculează statisticile curente pentru un lot
     */
    public LotStatistics calculateLotStatistics(String lotNumber) {
        ChicksLot lot = chicksLotRepository.findById(lotNumber)
                .orElseThrow(() -> new IllegalArgumentException("Lot not found"));

        List<ObservationSheet> observations = observationSheetRepository
                .findByLotIdOrderByWeekNumberDesc(lotNumber);

        Optional<ObservationSheet> latestObservation = observations.stream()
                .findFirst();

        int currentAgeInDays = lot.getDaysInFarm();
        int currentQuantity = lot.getCurrentQuantity();
        int totalMortality = lot.getInitialQuantity() - currentQuantity;

        BigDecimal averageWeight = latestObservation
                .map(ObservationSheet::getAverageWeight)
                .orElse(BigDecimal.ZERO);

        BigDecimal fcr = latestObservation
                .map(ObservationSheet::getFcr)
                .orElse(BigDecimal.ZERO);

        BigDecimal adg = latestObservation
                .map(ObservationSheet::getAdg)
                .orElse(BigDecimal.ZERO);

        return new LotStatistics(
                lot.getLotNumber(),
                currentAgeInDays,
                lot.getInitialQuantity(),
                currentQuantity,
                totalMortality,
                averageWeight,
                fcr,
                adg,
                lot.getReceptionDate(),
                lot.getExpectedSlaughterDate()
        );
    }

    /**
     * Calculează metricile de performanță pentru observația curentă
     */
    public PerformanceMetrics calculatePerformanceMetrics(ObservationSheet observation) {
        if (observation.getStatus() != ObservationSheetStatus.DRAFT) {
            throw new IllegalStateException("Can only calculate metrics for draft observations");
        }

        Optional<ObservationSheet> previousObservation = observationSheetRepository
                .findPreviousWeekObservation(observation.getLot().getLotNumber(), observation.getWeekNumber());

        BigDecimal previousWeight = previousObservation
                .map(ObservationSheet::getAverageWeight)
                .orElse(BigDecimal.ZERO);

        // Folosește metoda din entitate pentru calcul
        observation.calculatePerformanceMetrics(previousWeight);

        observationSheetRepository.save(observation);

        return new PerformanceMetrics(
                observation.getLot().getLotNumber(),
                observation.getEndDate(),
                observation.getWeekNumber(),
                observation.getWeightGain(),
                observation.getFcr(),
                observation.getAdg(),
                observation.getMortalityRate(),
                BigDecimal.valueOf(observation.getTotalWaterConsumed())
        );
    }

    /**
     * Analizează performanța tuturor loturilor active pentru o fermă
     */
    public List<LotPerformanceSummary> analyzeFarmPerformance(Long farmId) {
        List<ChicksLot> activeLots = chicksLotRepository.findActiveLotsForFarm(farmId);

        return activeLots.stream()
                .map(lot -> {
                    LotStatistics stats = calculateLotStatistics(lot.getLotNumber());
                    return new LotPerformanceSummary(lot, stats);
                })
                .toList();
    }

    /**
     * Verifică alerte de performanță pentru un lot
     */
    public PerformanceAlerts checkPerformanceAlerts(String lotNumber,
                                                    BigDecimal maxFcrThreshold,
                                                    BigDecimal maxMortalityThreshold) {
        ChicksLot lot = chicksLotRepository.findById(lotNumber)
                .orElseThrow(() -> new IllegalArgumentException("Lot not found"));

        Optional<ObservationSheet> latestObservation = observationSheetRepository
                .findLatestApprovedByLotId(lotNumber);

        List<String> alerts = new java.util.ArrayList<>();

        latestObservation.ifPresent(obs -> {
            if (obs.isFcrOutOfRange(maxFcrThreshold)) {
                alerts.add("FCR exceeds threshold: " + obs.getFcr());
            }

            if (obs.isMortalityHigh(BigDecimal.valueOf(lot.getExpectedMortalityRate()),
                    BigDecimal.valueOf(2.0))) {
                alerts.add("Mortality rate is high: " + obs.getMortalityRate() + "%");
            }

            if (obs.hasHighWeightVariance(BigDecimal.valueOf(20.0))) {
                alerts.add("High weight variance detected");
            }
        });

        return new PerformanceAlerts(lotNumber, alerts);
    }

    // DTO-uri pentru serviciu
    public record LotStatistics(
            String lotNumber,
            int currentAgeInDays,
            int initialQuantity,
            int currentQuantity,
            int totalMortality,
            BigDecimal averageWeight,
            BigDecimal feedConversionRatio,
            BigDecimal averageDailyGain,
            LocalDate receptionDate,
            LocalDate expectedSlaughterDate
    ) {}

    public record PerformanceMetrics(
            String lotNumber,
            LocalDate observationDate,
            int weekNumber,
            BigDecimal weightGain,
            BigDecimal feedConversionRatio,
            BigDecimal averageDailyGain,
            BigDecimal mortalityRate,
            BigDecimal waterConsumption
    ) {}

    public record LotPerformanceSummary(
            ChicksLot lot,
            LotStatistics statistics
    ) {}

    public record PerformanceAlerts(
            String lotId,
            List<String> alerts
    ) {}
}