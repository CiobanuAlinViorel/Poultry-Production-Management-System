package com.ppms.parentflock.service;

import com.ppms.parentflock.config.PfSettingKeys;
import com.ppms.parentflock.domain.DailyProductionSheet;
import com.ppms.parentflock.domain.Flock;
import com.ppms.parentflock.dtos.CreateDailySheetRequest;
import com.ppms.parentflock.dtos.DailyProductionSheetResponse;
import com.ppms.parentflock.repository.DailyProductionSheetRepository;
import com.ppms.parentflock.repository.FlockRepository;
import com.ppms.shared.domain.ScopeType;
import com.ppms.shared.exception.BusinessRuleViolationException;
import com.ppms.shared.exception.EntityNotFoundException;
import com.ppms.shared.settings.SettingsClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class DailyProductionService {

    private final DailyProductionSheetRepository sheetRepository;
    private final FlockRepository flockRepository;
    private final SettingsClient settingsClient;

    public DailyProductionService(DailyProductionSheetRepository sheetRepository,
                                    FlockRepository flockRepository,
                                    SettingsClient settingsClient) {
        this.sheetRepository = sheetRepository;
        this.flockRepository = flockRepository;
        this.settingsClient = settingsClient;
    }

    public DailyProductionSheetResponse recordDailySheet(UUID flockId, CreateDailySheetRequest request) {
        Flock flock = flockRepository.findById(flockId)
                .orElseThrow(() -> new EntityNotFoundException("Flock", flockId));
        if (flock.getStatus() == Flock.FlockStatus.CLOSED) {
            throw new BusinessRuleViolationException("FLOCK_CLOSED", "Cannot record sheet for a closed flock");
        }
        int backfillDays = settingsClient.get(
                PfSettingKeys.DAILY_SHEET_BACKFILL_DAYS,
                ScopeType.GLOBAL, null, 14);
        LocalDate today = LocalDate.now();
        if (request.date().isAfter(today) || ChronoUnit.DAYS.between(request.date(), today) > backfillDays) {
            throw new BusinessRuleViolationException("PF_SHEET_DATE_OUT_OF_RANGE",
                    "Sheet date must be within the last " + backfillDays + " days and not in the future");
        }
        DailyProductionSheet sheet = new DailyProductionSheet();
        sheet.setFlockId(flockId);
        sheet.setDate(request.date());
        sheet.setEggsTotal(request.eggsTotal());
        sheet.setEggsCracked(request.eggsCracked());
        sheet.setEggsDirty(request.eggsDirty());
        sheet.setEggsSmall(request.eggsSmall());
        sheet.setEggsDoubleYolk(request.eggsDoubleYolk());
        sheet.setFeedKg(request.feedKg());
        sheet.setWaterLiters(request.waterLiters());
        sheet.setMortality(request.mortality());
        DailyProductionSheet saved = sheetRepository.save(sheet);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<DailyProductionSheetResponse> listByFlock(UUID flockId) {
        if (!flockRepository.existsById(flockId)) {
            throw new EntityNotFoundException("Flock", flockId);
        }
        return sheetRepository.findByFlockIdOrderByDateDesc(flockId).stream()
                .map(this::toResponse)
                .toList();
    }

    private DailyProductionSheetResponse toResponse(DailyProductionSheet sheet) {
        return new DailyProductionSheetResponse(
                sheet.getId(),
                sheet.getFlockId(),
                sheet.getDate(),
                sheet.getEggsTotal(),
                sheet.getEggsCracked(),
                sheet.getEggsDirty(),
                sheet.getEggsSmall(),
                sheet.getEggsDoubleYolk(),
                sheet.getFeedKg(),
                sheet.getWaterLiters(),
                sheet.getMortality(),
                sheet.getCreatedAt(),
                sheet.getUpdatedAt()
        );
    }
}
