package com.example.broilerfarm.services;

import com.example.broilerfarm.domain.entities.ChicksLot;
import com.example.broilerfarm.domain.entities.ObservationSheet;
import com.example.broilerfarm.infrastructure.persistence.repositories.ObservationSheetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Domain Service pentru verificarea eligibilității de livrare
 * REGULI DE BUSINESS:
 * - Validarea criteriilor complexe pentru livrare
 * - Calculul prognozelor de eligibilitate
 * - Evaluarea riscurilor calitative
 */
@Service
@RequiredArgsConstructor
public class DeliveryEligibilityService {

    private final TreatmentWithdrawalService withdrawalService;
    private final ObservationSheetRepository observationSheetRepository;

    // Business rules configuration
    private static final int MINIMUM_AGE_DAYS = 35;
    private static final int MAXIMUM_AGE_DAYS = 50;
    private static final int MINIMUM_QUANTITY = 500;
    private static final BigDecimal MINIMUM_WEIGHT_KG = new BigDecimal("1.8");
    private static final BigDecimal MAXIMUM_FCR = new BigDecimal("2.0");
    private static final BigDecimal MAXIMUM_MORTALITY_RATE = new BigDecimal("8.0");
    private static final int OBSERVATION_MAX_AGE_DAYS = 7;

    /**
     * REGULĂ DE BUSINESS: Evaluarea completă a eligibilității pentru livrare
     */
    public DeliveryEligibilityAssessment assessEligibility(ChicksLot lot) {
        List<EligibilityCriterion> criteria = evaluateAllCriteria(lot);

        boolean isEligible = criteria.stream()
                .allMatch(c -> c.status() != CriterionStatus.BLOCKING);

        List<EligibilityCriterion> blockingIssues = criteria.stream()
                .filter(c -> c.status() == CriterionStatus.BLOCKING)
                .toList();

        List<EligibilityCriterion> warnings = criteria.stream()
                .filter(c -> c.status() == CriterionStatus.WARNING)
                .toList();

        ObservationSheet latestObservation = observationSheetRepository
                .findLatestApprovedByLotId(lot.getLotNumber())
                .orElse(null);

        return new DeliveryEligibilityAssessment(
                lot.getLotNumber(),
                isEligible,
                criteria,
                blockingIssues,
                warnings,
                latestObservation,
                calculateOverallScore(criteria),
                generateRecommendations(criteria)
        );
    }

    /**
     * REGULĂ DE BUSINESS: Verificare rapidă de eligibilitate
     */
    public boolean isLotEligibleForDelivery(ChicksLot lot) {
        return !hasBlockingIssues(lot);
    }

    /**
     * REGULĂ DE BUSINESS: Prognoză eligibilitate cu opțiuni de livrare
     */
    public DeliveryEligibilityForecast forecastEligibility(ChicksLot lot, int forecastDays) {
        DeliveryEligibilityAssessment currentAssessment = assessEligibility(lot);
        LocalDate estimatedDate = calculateEstimatedEligibilityDate(lot);

        List<DeliveryDateOption> dateOptions = generateDeliveryDateOptions(lot, forecastDays);

        return new DeliveryEligibilityForecast(
                lot.getLotNumber(),
                currentAssessment,
                estimatedDate,
                dateOptions,
                forecastDays
        );
    }

    /**
     * REGULĂ DE BUSINESS: Evaluare risc calitativ pentru livrare
     */
    public QualityRiskAssessment assessQualityRisks(ChicksLot lot) {
        DeliveryEligibilityAssessment eligibility = assessEligibility(lot);
        ObservationSheet latestObservation = eligibility.latestObservation();

        List<QualityRisk> risks = new ArrayList<>();

        if (latestObservation != null) {
            // Risk: High FCR
            if (latestObservation.getFcr() != null &&
                    latestObservation.getFcr().compareTo(new BigDecimal("1.8")) > 0) {
                risks.add(new QualityRisk(
                        "HIGH_FCR",
                        "Feed conversion ratio above optimal",
                        latestObservation.getFcr().compareTo(new BigDecimal("2.0")) > 0 ?
                                RiskLevel.HIGH : RiskLevel.MEDIUM,
                        "Consider extending growing period for better efficiency"
                ));
            }

            // Risk: Weight variance
            if (latestObservation.hasHighWeightVariance(new BigDecimal("15.0"))) {
                risks.add(new QualityRisk(
                        "HIGH_WEIGHT_VARIANCE",
                        "Uneven bird weights detected",
                        RiskLevel.MEDIUM,
                        "May affect processing efficiency and product consistency"
                ));
            }

            // Risk: Age near maximum
            if (lot.getDaysInFarm() > 45) {
                risks.add(new QualityRisk(
                        "ADVANCED_AGE",
                        "Lot approaching maximum recommended age",
                        RiskLevel.MEDIUM,
                        "Quality deterioration risk increases after 50 days"
                ));
            }
        }

        return new QualityRiskAssessment(
                lot.getLotNumber(),
                risks,
                calculateOverallRiskLevel(risks),
                generateRiskMitigation(risks)
        );
    }

    // Private business logic methods
    private List<EligibilityCriterion> evaluateAllCriteria(ChicksLot lot) {
        List<EligibilityCriterion> criteria = new ArrayList<>();

        // 1. Age criteria
        criteria.add(evaluateAgeCriterion(lot));

        // 2. Withdrawal period criteria
        criteria.add(evaluateWithdrawalCriterion(lot));

        // 3. Quantity criteria
        criteria.add(evaluateQuantityCriterion(lot));

        // 4. Observation criteria
        criteria.addAll(evaluateObservationCriteria(lot));

        return criteria;
    }

    private EligibilityCriterion evaluateAgeCriterion(ChicksLot lot) {
        int age = lot.getDaysInFarm();

        if (age < MINIMUM_AGE_DAYS) {
            return new EligibilityCriterion(
                    "MINIMUM_AGE",
                    String.format("Age %d days < minimum %d days", age, MINIMUM_AGE_DAYS),
                    CriterionStatus.BLOCKING,
                    String.format("Wait %d more days", MINIMUM_AGE_DAYS - age)
            );
        } else if (age > MAXIMUM_AGE_DAYS) {
            return new EligibilityCriterion(
                    "MAXIMUM_AGE",
                    String.format("Age %d days > recommended %d days", age, MAXIMUM_AGE_DAYS),
                    CriterionStatus.WARNING,
                    "Quality may be affected"
            );
        } else {
            return new EligibilityCriterion(
                    "MINIMUM_AGE",
                    String.format("Age %d days meets requirements", age),
                    CriterionStatus.PASSED,
                    "Age criterion satisfied"
            );
        }
    }

    private EligibilityCriterion evaluateWithdrawalCriterion(ChicksLot lot) {
        if (withdrawalService.hasActiveWithdrawalPeriod(lot.getLotNumber())) {
            LocalDate allowedDate = withdrawalService.getEarliestSlaughterDate(lot.getLotNumber());
            int daysRemaining = withdrawalService.analyzeWithdrawalStatus(lot.getLotNumber()).daysUntilEligible();

            return new EligibilityCriterion(
                    "WITHDRAWAL_PERIOD",
                    String.format("Active withdrawal period - %d days remaining", daysRemaining),
                    CriterionStatus.BLOCKING,
                    String.format("Earliest delivery date: %s", allowedDate)
            );
        } else {
            return new EligibilityCriterion(
                    "WITHDRAWAL_PERIOD",
                    "No active withdrawal periods",
                    CriterionStatus.PASSED,
                    "Withdrawal criterion satisfied"
            );
        }
    }

    private EligibilityCriterion evaluateQuantityCriterion(ChicksLot lot) {
        if (lot.getCurrentQuantity() < MINIMUM_QUANTITY) {
            return new EligibilityCriterion(
                    "MINIMUM_QUANTITY",
                    String.format("Quantity %d < minimum %d", lot.getCurrentQuantity(), MINIMUM_QUANTITY),
                    CriterionStatus.BLOCKING,
                    "Below minimum delivery quantity"
            );
        } else {
            return new EligibilityCriterion(
                    "MINIMUM_QUANTITY",
                    String.format("Quantity %d meets minimum", lot.getCurrentQuantity()),
                    CriterionStatus.PASSED,
                    "Quantity criterion satisfied"
            );
        }
    }

    private List<EligibilityCriterion> evaluateObservationCriteria(ChicksLot lot) {
        List<EligibilityCriterion> criteria = new ArrayList<>();
        ObservationSheet latestObservation = observationSheetRepository
                .findLatestApprovedByLotId(lot.getLotNumber())
                .orElse(null);

        if (latestObservation == null) {
            criteria.add(new EligibilityCriterion(
                    "OBSERVATION_SHEET",
                    "No approved observation sheet found",
                    CriterionStatus.BLOCKING,
                    "Create and approve observation sheet first"
            ));
            return criteria;
        }

        // Observation age
        long daysSinceObservation = java.time.temporal.ChronoUnit.DAYS.between(
                latestObservation.getEndDate(), LocalDate.now());

        if (daysSinceObservation > OBSERVATION_MAX_AGE_DAYS) {
            criteria.add(new EligibilityCriterion(
                    "OBSERVATION_AGE",
                    String.format("Observation %d days old > %d days max",
                            daysSinceObservation, OBSERVATION_MAX_AGE_DAYS),
                    CriterionStatus.WARNING,
                    "Consider creating new observation"
            ));
        }

        // Weight check
        if (latestObservation.getAverageWeight().compareTo(MINIMUM_WEIGHT_KG) < 0) {
            criteria.add(new EligibilityCriterion(
                    "MINIMUM_WEIGHT",
                    String.format("Weight %.3f kg < minimum %.1f kg",
                            latestObservation.getAverageWeight(), MINIMUM_WEIGHT_KG),
                    CriterionStatus.WARNING,
                    "Below optimal weight for delivery"
            ));
        }

        // FCR check
        if (latestObservation.getFcr() != null &&
                latestObservation.getFcr().compareTo(MAXIMUM_FCR) > 0) {
            criteria.add(new EligibilityCriterion(
                    "FCR_THRESHOLD",
                    String.format("FCR %.2f > maximum %.1f",
                            latestObservation.getFcr(), MAXIMUM_FCR),
                    CriterionStatus.WARNING,
                    "Performance below expected levels"
            ));
        }

        return criteria;
    }

    private boolean hasBlockingIssues(ChicksLot lot) {
        return evaluateAllCriteria(lot).stream()
                .anyMatch(c -> c.status() == CriterionStatus.BLOCKING);
    }

    private LocalDate calculateEstimatedEligibilityDate(ChicksLot lot) {
        LocalDate ageDate = lot.getReceptionDate().plusDays(MINIMUM_AGE_DAYS);
        LocalDate withdrawalDate = withdrawalService.getEarliestSlaughterDate(lot.getLotNumber());

        LocalDate maxDate = withdrawalDate != null && withdrawalDate.isAfter(ageDate) ?
                withdrawalDate : ageDate;

        return maxDate.isBefore(LocalDate.now()) ? LocalDate.now() : maxDate;
    }

    private List<DeliveryDateOption> generateDeliveryDateOptions(ChicksLot lot, int forecastDays) {
        List<DeliveryDateOption> options = new ArrayList<>();
        LocalDate startDate = LocalDate.now();

        for (int i = 0; i <= forecastDays; i++) {
            LocalDate deliveryDate = startDate.plusDays(i);
            boolean isEligible = isDateEligibleForDelivery(lot, deliveryDate);

            options.add(new DeliveryDateOption(
                    deliveryDate,
                    isEligible,
                    i,
                    generateDateRecommendation(lot, deliveryDate)
            ));
        }

        return options;
    }

    private boolean isDateEligibleForDelivery(ChicksLot lot, LocalDate date) {
        LocalDate ageDate = lot.getReceptionDate().plusDays(MINIMUM_AGE_DAYS);
        LocalDate withdrawalDate = withdrawalService.getEarliestSlaughterDate(lot.getLotNumber());

        boolean ageOk = !date.isBefore(ageDate);
        boolean withdrawalOk = withdrawalDate == null || !date.isBefore(withdrawalDate);

        return ageOk && withdrawalOk;
    }

    private String generateDateRecommendation(ChicksLot lot, LocalDate date) {
        if (!isDateEligibleForDelivery(lot, date)) {
            LocalDate ageDate = lot.getReceptionDate().plusDays(MINIMUM_AGE_DAYS);
            LocalDate withdrawalDate = withdrawalService.getEarliestSlaughterDate(lot.getLotNumber());

            if (date.isBefore(ageDate)) {
                return "Wait for minimum age requirement";
            } else if (withdrawalDate != null && date.isBefore(withdrawalDate)) {
                return "Wait for withdrawal period to end";
            }
        }
        return "Eligible for delivery";
    }

    private int calculateOverallScore(List<EligibilityCriterion> criteria) {
        long passedCount = criteria.stream()
                .filter(c -> c.status() == CriterionStatus.PASSED)
                .count();
        return (int) ((double) passedCount / criteria.size() * 100);
    }

    private List<String> generateRecommendations(List<EligibilityCriterion> criteria) {
        return criteria.stream()
                .filter(c -> c.status() != CriterionStatus.PASSED)
                .map(EligibilityCriterion::recommendation)
                .toList();
    }

    private RiskLevel calculateOverallRiskLevel(List<QualityRisk> risks) {
        if (risks.stream().anyMatch(r -> r.riskLevel() == RiskLevel.HIGH)) {
            return RiskLevel.HIGH;
        } else if (risks.stream().anyMatch(r -> r.riskLevel() == RiskLevel.MEDIUM)) {
            return RiskLevel.MEDIUM;
        } else {
            return RiskLevel.LOW;
        }
    }

    private List<String> generateRiskMitigation(List<QualityRisk> risks) {
        return risks.stream()
                .map(QualityRisk::mitigation)
                .toList();
    }

    // DTO-uri pentru Domain Service
    public record DeliveryEligibilityAssessment(
            String lotNumber,
            boolean isEligible,
            List<EligibilityCriterion> criteria,
            List<EligibilityCriterion> blockingIssues,
            List<EligibilityCriterion> warnings,
            ObservationSheet latestObservation,
            int overallScore,
            List<String> recommendations
    ) {}

    public record EligibilityCriterion(
            String criterionCode,
            String description,
            CriterionStatus status,
            String recommendation
    ) {}

    public enum CriterionStatus {
        PASSED, WARNING, BLOCKING
    }

    public record DeliveryEligibilityForecast(
            String lotNumber,
            DeliveryEligibilityAssessment currentAssessment,
            LocalDate estimatedEligibilityDate,
            List<DeliveryDateOption> deliveryOptions,
            int forecastPeriodDays
    ) {}

    public record DeliveryDateOption(
            LocalDate deliveryDate,
            boolean isEligible,
            int daysFromNow,
            String recommendation
    ) {}

    public record QualityRiskAssessment(
            String lotNumber,
            List<QualityRisk> risks,
            RiskLevel overallRiskLevel,
            List<String> mitigationStrategies
    ) {}

    public record QualityRisk(
            String riskCode,
            String description,
            RiskLevel riskLevel,
            String mitigation
    ) {}

    public enum RiskLevel {
        LOW, MEDIUM, HIGH
    }
}