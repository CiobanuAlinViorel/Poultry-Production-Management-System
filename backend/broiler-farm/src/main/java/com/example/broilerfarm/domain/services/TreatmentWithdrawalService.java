package com.example.broilerfarm.domain.services;

import com.example.broilerfarm.domain.entities.TreatmentSheet;
import com.example.broilerfarm.domain.entities.TreatmentSheetLine;
import com.example.broilerfarm.infrastructure.persistence.repositories.TreatmentSheetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Domain Service pentru gestionarea perioadelor de retragere (withdrawal)
 * REGULI DE BUSINESS:
 * - Verificarea eligibilității pentru livrare bazată pe withdrawal periods
 * - Managementul interacțiunilor între medicamente
 * - Calculul perioadelor de așteptare pentru tăiere
 */
@Service
@RequiredArgsConstructor
public class TreatmentWithdrawalService {

    private final TreatmentSheetRepository treatmentSheetRepository;

    /**
     * REGULĂ DE BUSINESS: Verifică dacă un lot are perioadă de retragere activă
     */
    public boolean hasActiveWithdrawalPeriod(Long lotId) {
        List<TreatmentSheet> treatments = treatmentSheetRepository.findTreatmentsForWithdrawalCheck(lotId);

        return treatments.stream()
                .flatMap(t -> t.getTreatmentLines().stream())
                .anyMatch(line -> isWithdrawalActive(line));
    }

    /**
     * REGULĂ DE BUSINESS: Obține data cea mai apropiată pentru tăiere permisă
     */
    public LocalDate getEarliestSlaughterDate(Long lotId) {
        List<TreatmentSheet> treatments = treatmentSheetRepository.findTreatmentsForWithdrawalCheck(lotId);

        return treatments.stream()
                .flatMap(t -> t.getTreatmentLines().stream())
                .map(TreatmentSheetLine::getSlaughterAllowedDate)
                .max(LocalDate::compareTo)
                .orElse(LocalDate.now()); // Dacă nu există tratamente, poate fi tăiat imediat
    }

    /**
     * REGULĂ DE BUSINESS: Analiză completă a stării de withdrawal
     */
    public WithdrawalAnalysis analyzeWithdrawalStatus(Long lotId) {
        List<TreatmentSheet> treatments = treatmentSheetRepository.findTreatmentsForWithdrawalCheck(lotId);

        List<ActiveWithdrawal> activeWithdrawals = treatments.stream()
                .flatMap(treatment -> treatment.getTreatmentLines().stream())
                .filter(this::isWithdrawalActive)
                .map(this::mapToActiveWithdrawal)
                .toList();

        boolean hasActiveWithdrawal = !activeWithdrawals.isEmpty();
        LocalDate earliestSlaughterDate = getEarliestSlaughterDate(lotId);
        boolean isEligibleForDelivery = !hasActiveWithdrawal;
        int daysUntilEligible = calculateDaysUntilEligible(earliestSlaughterDate);

        return new WithdrawalAnalysis(
                lotId,
                hasActiveWithdrawal,
                isEligibleForDelivery,
                earliestSlaughterDate,
                daysUntilEligible,
                activeWithdrawals
        );
    }

    /**
     * REGULĂ DE BUSINESS: Verifică eligibilitatea pentru livrare
     */
    public boolean isLotEligibleForDelivery(Long lotId) {
        return !hasActiveWithdrawalPeriod(lotId);
    }

    /**
     * REGULĂ DE BUSINESS: Verifică conflicte potențiale între medicamente
     */
    public DrugInteractionCheck checkDrugInteractions(Long lotId, Long newMedicationId) {
        List<TreatmentSheet> currentTreatments = treatmentSheetRepository.findTreatmentsForWithdrawalCheck(lotId);

        List<String> currentMedications = currentTreatments.stream()
                .flatMap(t -> t.getTreatmentLines().stream())
                .map(line -> line.getMedication().getName())
                .distinct()
                .toList();

        // TODO: Integrare cu sistem extern de verificare interacțiuni medicamentoase
        boolean hasKnownInteractions = checkKnownInteractions(currentMedications, newMedicationId);
        String recommendation = hasKnownInteractions ?
                "CONSULTĂ VETERINARUL - Interacțiuni potențiale detectate" :
                "Tratament permis conform bazei de date curente";

        return new DrugInteractionCheck(
                lotId,
                newMedicationId,
                !hasKnownInteractions,
                recommendation,
                currentMedications
        );
    }

    /**
     * REGULĂ DE BUSINESS: Prognoză disponibilitate livrare
     */
    public DeliveryEligibilityForecast getDeliveryEligibilityForecast(Long lotId, int forecastDays) {
        WithdrawalAnalysis currentStatus = analyzeWithdrawalStatus(lotId);

        List<DeliveryDateOption> deliveryOptions = LocalDate.now()
                .datesUntil(LocalDate.now().plusDays(forecastDays + 1))
                .map(date -> new DeliveryDateOption(
                        date,
                        isDateEligibleForDelivery(lotId, date),
                        calculateDaysUntilDate(date)
                ))
                .toList();

        return new DeliveryEligibilityForecast(
                lotId,
                currentStatus,
                deliveryOptions,
                forecastDays
        );
    }

    // Private business logic methods
    private boolean isWithdrawalActive(TreatmentSheetLine line) {
        return line.getSlaughterAllowedDate().isAfter(LocalDate.now());
    }

    private ActiveWithdrawal mapToActiveWithdrawal(TreatmentSheetLine line) {
        LocalDate today = LocalDate.now();
        int daysRemaining = (int) java.time.temporal.ChronoUnit.DAYS.between(
                today, line.getSlaughterAllowedDate());

        return new ActiveWithdrawal(
                line.getTreatmentSheet().getId(),
                line.getMedication().getName(),
                line.getTreatmentSheet().getDiagnosis(),
                line.getEndDate(),
                line.getWithdrawalPeriod(),
                line.getSlaughterAllowedDate(),
                daysRemaining
        );
    }

    private int calculateDaysUntilEligible(LocalDate earliestSlaughterDate) {
        if (earliestSlaughterDate.isBefore(LocalDate.now()) || earliestSlaughterDate.isEqual(LocalDate.now())) {
            return 0;
        }
        return (int) java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), earliestSlaughterDate);
    }

    private boolean isDateEligibleForDelivery(Long lotId, LocalDate date) {
        LocalDate earliestSlaughterDate = getEarliestSlaughterDate(lotId);
        return earliestSlaughterDate.isBefore(date) || earliestSlaughterDate.isEqual(date);
    }

    private int calculateDaysUntilDate(LocalDate date) {
        return (int) java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), date);
    }

    private boolean checkKnownInteractions(List<String> currentMedications, Long newMedicationId) {
        // TODO: Implementare logică complexă de verificare interacțiuni
        // Temporar - consideră că nu există interacțiuni cunoscute
        return false;
    }

    // DTO-uri pentru Domain Service
    public record WithdrawalAnalysis(
            Long lotId,
            boolean hasActiveWithdrawal,
            boolean isEligibleForDelivery,
            LocalDate earliestSlaughterDate,
            int daysUntilEligible,
            List<ActiveWithdrawal> activeWithdrawals
    ) {}

    public record ActiveWithdrawal(
            Long treatmentSheetId,
            String medicationName,
            String diagnosis,
            LocalDate treatmentEndDate,
            int withdrawalPeriodDays,
            LocalDate slaughterAllowedDate,
            int daysRemaining
    ) {}

    public record DrugInteractionCheck(
            Long lotId,
            Long newMedicationId,
            boolean isSafeToAdminister,
            String recommendation,
            List<String> currentMedications
    ) {}

    public record DeliveryEligibilityForecast(
            Long lotId,
            WithdrawalAnalysis currentStatus,
            List<DeliveryDateOption> deliveryOptions,
            int forecastPeriodDays
    ) {}

    public record DeliveryDateOption(
            LocalDate deliveryDate,
            boolean isEligible,
            int daysFromNow
    ) {}
}