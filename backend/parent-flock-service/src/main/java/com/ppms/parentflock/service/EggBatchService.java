package com.ppms.parentflock.service;

import com.ppms.parentflock.config.PfSettingKeys;
import com.ppms.parentflock.domain.EggBatch;
import com.ppms.parentflock.dtos.CreateEggBatchRequest;
import com.ppms.parentflock.dtos.EggBatchResponse;
import com.ppms.parentflock.repository.EggBatchRepository;
import com.ppms.parentflock.repository.FlockRepository;
import com.ppms.shared.domain.ScopeType;
import com.ppms.shared.exception.BusinessRuleViolationException;
import com.ppms.shared.exception.EntityNotFoundException;
import com.ppms.shared.settings.SettingsClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class EggBatchService {

    private final EggBatchRepository eggBatchRepository;
    private final FlockRepository flockRepository;
    private final SettingsClient settingsClient;

    public EggBatchService(EggBatchRepository eggBatchRepository,
                           FlockRepository flockRepository,
                           SettingsClient settingsClient) {
        this.eggBatchRepository = eggBatchRepository;
        this.flockRepository = flockRepository;
        this.settingsClient = settingsClient;
    }

    public EggBatchResponse create(CreateEggBatchRequest request) {
        if (!flockRepository.existsById(request.flockId())) {
            throw new EntityNotFoundException("Flock", request.flockId());
        }
        int maxStorageDays = settingsClient.get(
                PfSettingKeys.EGG_MAX_STORAGE_DAYS,
                ScopeType.GLOBAL, null, 10);
        if (request.storageDays() > maxStorageDays) {
            throw new BusinessRuleViolationException("PF_EGG_MAX_STORAGE_DAYS_EXCEEDED",
                    "Storage days exceed maximum allowed: " + maxStorageDays);
        }
        EggBatch batch = new EggBatch();
        batch.setFlockId(request.flockId());
        batch.setBatchCode(request.batchCode());
        batch.setLaidDate(request.laidDate());
        batch.setStorageDays(request.storageDays());
        batch.setQualityGrade(request.qualityGrade());
        batch.setQuantity(request.quantity());
        batch.setReservedFor(request.reservedFor());
        EggBatch saved = eggBatchRepository.save(batch);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<EggBatchResponse> list() {
        return eggBatchRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    private EggBatchResponse toResponse(EggBatch batch) {
        return new EggBatchResponse(
                batch.getId(),
                batch.getFlockId(),
                batch.getBatchCode(),
                batch.getLaidDate(),
                batch.getStorageDays(),
                batch.getQualityGrade(),
                batch.getQuantity(),
                batch.getReservedFor(),
                batch.getStatus(),
                batch.getCreatedAt(),
                batch.getUpdatedAt()
        );
    }
}
