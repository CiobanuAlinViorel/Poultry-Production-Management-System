package com.example.broilerfarm.services;

import com.example.broilerfarm.domain.entities.ConsumptionSheet;
import com.example.broilerfarm.domain.entities.MortalitySheet;
import com.example.broilerfarm.domain.entities.ObservationSheet;
import com.example.broilerfarm.infrastructure.persistence.repositories.ConsumptionSheetRepository;
import com.example.broilerfarm.infrastructure.persistence.repositories.MortalitySheetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * Domain Service pentru agregarea datelor săptămânale
// * UC-06: Send the Consumptions Informations
 */
@Service
@RequiredArgsConstructor
public class ObservationDataAggregationService {

    private final MortalitySheetRepository mortalitySheetRepository;
    private final ConsumptionSheetRepository consumptionSheetRepository;

    /**
     * Populează ObservationSheet cu date agregate din săptămână
     *
     * @param observation ObservationSheet de completat
     * @param previousWeekBirdCount Număr păsări din săptămâna anterioară
     * @param currentBirdCount Număr păsări curent
     */
    public void populateObservationFromWeeklyData(
            ObservationSheet observation,
            Integer previousWeekBirdCount,
            Integer currentBirdCount) {

        Long lotId = observation.getLot().getId();
        LocalDate startDate = observation.getStartDate();
        LocalDate endDate = observation.getEndDate();

        // Agregare mortalitate
        List<MortalitySheet> weeklyMortality =
                mortalitySheetRepository.findByLotIdAndDateRange(lotId, startDate, endDate);

        int totalMortality = weeklyMortality.stream()
                .mapToInt(MortalitySheet::getTotalMortality)
                .sum();

        BigDecimal avgDailyMortality = totalMortality > 0
                ? BigDecimal.valueOf(totalMortality).divide(BigDecimal.valueOf(7), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Agregare consum
        List<ConsumptionSheet> weeklyConsumption =
                consumptionSheetRepository.findByLotIdAndDateRange(lotId, startDate, endDate);

        int totalFeed = weeklyConsumption.stream()
                .map(ConsumptionSheet::getTotalFeedConsumed)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .intValue();

        int totalWater = weeklyConsumption.stream()
                .map(ConsumptionSheet::getTotalWaterConsumed)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .intValue();

        // Populare observation
        observation.autoPopulateFromDataSources(
                previousWeekBirdCount,
                currentBirdCount,
                totalMortality,
                totalFeed,
                totalWater
        );
    }

    /**
     * Calculează media zilnică de consum de hrană
     */
    public BigDecimal calculateAverageDailyFeedConsumption(Long lotId, LocalDate startDate, LocalDate endDate) {
        List<ConsumptionSheet> sheets =
                consumptionSheetRepository.findByLotIdAndDateRange(lotId, startDate, endDate);

        if (sheets.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalFeed = sheets.stream()
                .map(ConsumptionSheet::getTotalFeedConsumed)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return totalFeed.divide(BigDecimal.valueOf(sheets.size()), 2, RoundingMode.HALF_UP);
    }

    /**
     * Calculează rata medie zilnică de mortalitate
     */
    public BigDecimal calculateAverageDailyMortalityRate(Long lotId, LocalDate startDate, LocalDate endDate) {
        List<MortalitySheet> sheets =
                mortalitySheetRepository.findByLotIdAndDateRange(lotId, startDate, endDate);

        if (sheets.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalRate = sheets.stream()
                .map(MortalitySheet::getDailyMortalityRate)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return totalRate.divide(BigDecimal.valueOf(sheets.size()), 4, RoundingMode.HALF_UP);
    }

    /**
     * Identifică zilele lipsă din săptămână (date fără sheets)
     */
    public List<LocalDate> findMissingDays(Long lotId, LocalDate startDate, LocalDate endDate) {
        List<MortalitySheet> mortalitySheets =
                mortalitySheetRepository.findByLotIdAndDateRange(lotId, startDate, endDate);

        List<LocalDate> recordedDates = mortalitySheets.stream()
                .map(MortalitySheet::getSheetDate)
                .toList();

        return startDate.datesUntil(endDate.plusDays(1))
                .filter(date -> !recordedDates.contains(date))
                .toList();
    }
}