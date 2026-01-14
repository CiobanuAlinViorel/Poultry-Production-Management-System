package com.example.broilerfarm.services;

import com.example.broilerfarm.domain.entities.ChicksLot;
import com.example.broilerfarm.domain.entities.MortalitySheet;
import com.example.broilerfarm.infrastructure.persistence.repositories.ChicksLotRepository;
import com.example.broilerfarm.infrastructure.persistence.repositories.MortalitySheetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Domain Service pentru analiza mortalității
 * UC-012: Create the Mortality Sheet
 */
@Service
@RequiredArgsConstructor
public class MortalityAnalysisService {

    private final MortalitySheetRepository mortalitySheetRepository;
    private final ChicksLotRepository chicksLotRepository;

    /**
     * Analizează pattern-urile de mortalitate pentru un lot
     */
    public MortalityAnalysis analyzeMortalityPatterns(String lotNumber, LocalDate startDate, LocalDate endDate) {
        ChicksLot lot = chicksLotRepository.findById(lotNumber)
                .orElseThrow(() -> new IllegalArgumentException("Lot not found"));

        List<MortalitySheet> mortalitySheets = mortalitySheetRepository
                .findByLotIdAndDateRange(lotNumber, startDate, endDate);

        int totalMortality = mortalitySheets.stream()
                .mapToInt(MortalitySheet::getTotalMortality)
                .sum();

        BigDecimal averageDailyMortality = calculateAverageDailyMortality(mortalitySheets, startDate, endDate);
        BigDecimal mortalityRate = calculateMortalityRate(totalMortality, lot.getInitialQuantity());
        Map<String, Long> causesAnalysis = analyzeMortalityCauses(mortalitySheets);
        Map<Integer, Integer> agePattern = analyzeMortalityByAge(mortalitySheets);
        String trend = analyzeMortalityTrend(mortalitySheets);

        return new MortalityAnalysis(
                lotNumber,
                totalMortality,
                averageDailyMortality,
                mortalityRate,
                causesAnalysis,
                agePattern,
                trend,
                shouldTriggerAlert(lot, mortalityRate)
        );
    }

    /**
     * Verifică dacă trebuie declanșată alertă de mortalitate
     */
    public boolean shouldTriggerMortalityAlert(String lotNumber, BigDecimal thresholdPercentage) {
        ChicksLot lot = chicksLotRepository.findById(lotNumber)
                .orElseThrow(() -> new IllegalArgumentException("Lot not found"));

        List<MortalitySheet> recentSheets = mortalitySheetRepository
                .findByLotIdOrderByDateDesc(lotNumber)
                .stream()
                .limit(7) // Ultimele 7 zile
                .toList();

        int recentMortality = recentSheets.stream()
                .mapToInt(MortalitySheet::getTotalMortality)
                .sum();

        BigDecimal recentMortalityRate = calculateMortalityRate(recentMortality, lot.getCurrentQuantity());
        BigDecimal expectedRate = BigDecimal.valueOf(lot.getExpectedMortalityRate());

        return recentMortalityRate.compareTo(expectedRate.add(thresholdPercentage)) > 0;
    }

    /**
     * Găsește loturile cu mortalitate crescută pentru o fermă
     */
    public List<HighMortalityLot> findLotsWithHighMortality(Long farmId, BigDecimal threshold) {
        List<ChicksLot> activeLots = chicksLotRepository.findActiveLotsForFarm(farmId);

        return activeLots.stream()
                .map(lot -> {
                    List<MortalitySheet> recentSheets = mortalitySheetRepository
                            .findByLotIdOrderByDateDesc(lot.getLotNumber())
                            .stream()
                            .limit(3) // Ultimele 3 zile
                            .toList();

                    int recentMortality = recentSheets.stream()
                            .mapToInt(MortalitySheet::getTotalMortality)
                            .sum();

                    BigDecimal recentRate = calculateMortalityRate(recentMortality, lot.getCurrentQuantity());

                    return new HighMortalityLot(lot, recentRate, recentMortality);
                })
                .filter(lot -> lot.mortalityRate().compareTo(threshold) > 0)
                .toList();
    }

    /**
     * Agregă date de mortalitate pentru rapoarte săptămânale
     */
    public WeeklyMortalitySummary getWeeklyMortalitySummary(String lotNumber, LocalDate weekStart) {
        LocalDate weekEnd = weekStart.plusDays(6);

        List<MortalitySheet> weeklySheets = mortalitySheetRepository
                .findByLotIdAndDateRange(lotNumber, weekStart, weekEnd);

        int weeklyTotal = weeklySheets.stream()
                .mapToInt(MortalitySheet::getTotalMortality)
                .sum();

        Map<String, Integer> dailyBreakdown = weeklySheets.stream()
                .collect(Collectors.toMap(
                        sheet -> sheet.getSheetDate().toString(),
                        MortalitySheet::getTotalMortality
                ));

        Map<String, Long> causeBreakdown = analyzeMortalityCauses(weeklySheets);

        return new WeeklyMortalitySummary(
                lotNumber,
                weekStart,
                weekEnd,
                weeklyTotal,
                dailyBreakdown,
                causeBreakdown
        );
    }

    // Metode private helper
    private BigDecimal calculateAverageDailyMortality(List<MortalitySheet> sheets,
                                                      LocalDate startDate, LocalDate endDate) {
        if (sheets.isEmpty()) {
            return BigDecimal.ZERO;
        }

        long daysInPeriod = startDate.until(endDate).getDays() + 1;
        int totalMortality = sheets.stream()
                .mapToInt(MortalitySheet::getTotalMortality)
                .sum();

        return BigDecimal.valueOf(totalMortality)
                .divide(BigDecimal.valueOf(daysInPeriod), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateMortalityRate(int mortality, int totalBirds) {
        if (totalBirds == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(mortality)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalBirds), 2, RoundingMode.HALF_UP);
    }

    private Map<String, Long> analyzeMortalityCauses(List<MortalitySheet> sheets) {
        return sheets.stream()
                .filter(sheet -> sheet.getPrimaryCause() != null)
                .collect(Collectors.groupingBy(
                        MortalitySheet::getPrimaryCause,
                        Collectors.summingLong(MortalitySheet::getTotalMortality)
                ));
    }

    private Map<Integer, Integer> analyzeMortalityByAge(List<MortalitySheet> sheets) {
        return sheets.stream()
                .collect(Collectors.groupingBy(
                        MortalitySheet::getAgeInDays,
                        Collectors.summingInt(MortalitySheet::getTotalMortality)
                ));
    }

    private String analyzeMortalityTrend(List<MortalitySheet> sheets) {
        if (sheets.size() < 2) {
            return "INSUFFICIENT_DATA";
        }

        // Sortează după dată și analizează trendul
        List<MortalitySheet> sortedSheets = sheets.stream()
                .sorted((s1, s2) -> s1.getSheetDate().compareTo(s2.getSheetDate()))
                .toList();

        int firstPeriod = sortedSheets.subList(0, sortedSheets.size() / 2).stream()
                .mapToInt(MortalitySheet::getTotalMortality)
                .sum();

        int secondPeriod = sortedSheets.subList(sortedSheets.size() / 2, sortedSheets.size()).stream()
                .mapToInt(MortalitySheet::getTotalMortality)
                .sum();

        if (secondPeriod > firstPeriod * 1.2) {
            return "INCREASING";
        } else if (secondPeriod < firstPeriod * 0.8) {
            return "DECREASING";
        } else {
            return "STABLE";
        }
    }

    private boolean shouldTriggerAlert(ChicksLot lot, BigDecimal currentRate) {
        BigDecimal expectedRate = BigDecimal.valueOf(lot.getExpectedMortalityRate());
        BigDecimal tolerance = BigDecimal.valueOf(2.0); // 2% tolerance
        return currentRate.compareTo(expectedRate.add(tolerance)) > 0;
    }

    // DTO-uri pentru serviciu
    public record MortalityAnalysis(
            String lotNumber,
            int totalMortality,
            BigDecimal averageDailyMortality,
            BigDecimal mortalityRate,
            Map<String, Long> causesAnalysis,
            Map<Integer, Integer> agePattern,
            String trend,
            boolean requiresAttention
    ) {}

    public record HighMortalityLot(
            ChicksLot lot,
            BigDecimal mortalityRate,
            int recentMortality
    ) {}

    public record WeeklyMortalitySummary(
            String lotNumber,
            LocalDate weekStart,
            LocalDate weekEnd,
            int totalMortality,
            Map<String, Integer> dailyBreakdown,
            Map<String, Long> causeBreakdown
    ) {}
}