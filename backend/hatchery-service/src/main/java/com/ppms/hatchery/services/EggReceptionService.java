package com.ppms.hatchery.services;

import com.ppms.hatchery.domain.EggBatch;
import com.ppms.hatchery.domain.Hatchery;
import com.ppms.hatchery.dtos.EggBatchRequest;
import com.ppms.hatchery.repositories.EggBatchRepository;
import com.ppms.hatchery.repositories.HatcheryRepository;
import com.ppms.shared.events.DomainEvent;
import com.ppms.shared.events.OutboxPublisher;
import com.ppms.shared.events.Topics;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class EggReceptionService {

    private final EggBatchRepository eggBatchRepository;
    private final HatcheryRepository hatcheryRepository;
    private final OutboxPublisher outboxPublisher;

    public EggBatch receive(EggBatchRequest request) {
        Hatchery hatchery = hatcheryRepository.findById(request.hatcheryId())
                .orElseThrow(() -> new EntityNotFoundException("Hatchery not found"));
        if (eggBatchRepository.existsByBatchCodeIgnoreCase(request.batchCode())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Batch code already exists");
        }
        if (request.qtyAccepted() == null || request.qtyAccepted() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Accepted quantity must be greater than zero");
        }
        if (request.collectionDate() != null) {
            long ageDays = ChronoUnit.DAYS.between(request.collectionDate(), Instant.now());
            if (ageDays > 7) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "HA_EGG_TOO_OLD: eggs older than 7 days");
            }
        }
        EggBatch batch = new EggBatch();
        batch.setId(UUID.randomUUID());
        batch.setBatchCode(request.batchCode().trim());
        batch.setHatcheryId(request.hatcheryId());
        batch.setOriginFlockId(request.originFlockId());
        batch.setOriginSiteId(request.originSiteId());
        batch.setBreedCode(request.breedCode());
        batch.setCollectionDate(request.collectionDate());
        batch.setQtyAccepted(request.qtyAccepted());
        batch.setQtyAvailable(request.qtyAccepted());
        batch.setQtySet(0);
        batch.setStatus(EggBatch.Status.IN_STORE);
        batch.setCreatedAt(Instant.now());
        batch.setUpdatedAt(Instant.now());
        EggBatch saved = eggBatchRepository.save(batch);
        outboxPublisher.publish(Topics.HATCHERY_EGG_RECEPTION, new DomainEvent<>(UUID.randomUUID(), "hatchery.egg-batch.received", 1, "EggBatch", saved.getId(), hatchery.getId(), null,
                new EggBatchReceivedPayload(saved.getId(), saved.getBatchCode(), saved.getHatcheryId(), saved.getOriginFlockId(), saved.getQtyAccepted(), saved.getCollectionDate())));
        return saved;
    }

    public record EggBatchReceivedPayload(UUID eggBatchId, String batchCode, UUID hatcheryId, UUID originFlockId, int qtyAccepted, Instant collectionDate) {
    }
}
