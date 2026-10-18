package com.ppms.hatchery.services;

import com.ppms.hatchery.domain.*;
import com.ppms.hatchery.dtos.*;
import com.ppms.hatchery.repositories.*;
import com.ppms.shared.events.DomainEvent;
import com.ppms.shared.events.OutboxPublisher;
import com.ppms.shared.events.SiteDirectoryPayload;
import com.ppms.shared.events.Topics;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class HatcheryCoreService {

    private final HatcheryRepository hatcheryRepository;
    private final IncubatorRepository incubatorRepository;
    private final EggBatchRepository eggBatchRepository;
    private final IncubationBatchRepository incubationBatchRepository;
    private final ChickBatchRepository chickBatchRepository;
    private final OutboxPublisher outboxPublisher;

    public Hatchery createHatchery(HatcheryRequest request) {
        Hatchery h = new Hatchery();
        h.setId(UUID.randomUUID());
        h.setCode(request.code());
        h.setName(request.name());
        h.setAddress(request.address());
        h.setCounty(request.county());
        h.setWeeklySetCapacity(request.weeklySetCapacity());
        h.setActive(true);
        h.setCreatedAt(Instant.now());
        h.setUpdatedAt(Instant.now());
        Hatchery saved = hatcheryRepository.save(h);
        outboxPublisher.publish(Topics.SITE_DIRECTORY, new DomainEvent<>(UUID.randomUUID(), "site.directory.upserted", 1, "SiteDirectory", saved.getId(), saved.getId(), null,
                new SiteDirectoryPayload(saved.getId(), "HATCHERY", saved.getCode(), saved.getName(), saved.getAddress(), saved.getCounty(), saved.isActive(), "hatchery-service", Instant.now())));
        return saved;
    }

    public List<Hatchery> listHatcheries() {
        return hatcheryRepository.findAll();
    }

    public Incubator createIncubator(IncubatorRequest request) {
        Incubator i = new Incubator();
        i.setId(UUID.randomUUID());
        i.setHatcheryId(request.hatcheryId());
        i.setCode(request.code());
        i.setType(Incubator.Type.valueOf(request.type()));
        i.setManufacturer(request.manufacturer());
        i.setModel(request.model());
        i.setCapacityEggs(request.capacityEggs());
        i.setStatus(request.status() != null ? Incubator.Status.valueOf(request.status()) : Incubator.Status.AVAILABLE);
        i.setCreatedAt(Instant.now());
        i.setUpdatedAt(Instant.now());
        return incubatorRepository.save(i);
    }

    public List<Incubator> listIncubators(UUID hatcheryId) {
        return incubatorRepository.findByHatcheryId(hatcheryId);
    }

    public EggBatch createEggBatch(EggBatchRequest request) {
        EggBatch eb = new EggBatch();
        eb.setId(UUID.randomUUID());
        eb.setBatchCode(request.batchCode());
        eb.setHatcheryId(request.hatcheryId());
        eb.setOriginFlockId(request.originFlockId());
        eb.setOriginSiteId(request.originSiteId());
        eb.setBreedCode(request.breedCode());
        eb.setCollectionDate(request.collectionDate());
        eb.setQtyAccepted(request.qtyAccepted());
        eb.setQtyAvailable(request.qtyAccepted());
        eb.setQtySet(0);
        eb.setStatus(EggBatch.Status.IN_STORE);
        eb.setCreatedAt(Instant.now());
        eb.setUpdatedAt(Instant.now());
        return eggBatchRepository.save(eb);
    }

    public List<EggBatch> listEggBatches(UUID hatcheryId) {
        return eggBatchRepository.findByHatcheryId(hatcheryId);
    }

    public IncubationBatch createIncubationBatch(IncubationBatchRequest request) {
        IncubationBatch ib = new IncubationBatch();
        ib.setId(UUID.randomUUID());
        ib.setBatchCode(request.batchCode());
        ib.setHatcheryId(request.hatcheryId());
        ib.setSetterId(request.setterId());
        ib.setSetAt(request.setAt());
        ib.setExpectedHatchDate(request.expectedHatchDate());
        ib.setQtySet(request.qtySet());
        ib.setStatus(IncubationBatch.Status.SET);
        ib.setCreatedAt(Instant.now());
        ib.setUpdatedAt(Instant.now());
        return incubationBatchRepository.save(ib);
    }

    public List<IncubationBatch> listIncubationBatches(UUID hatcheryId) {
        return incubationBatchRepository.findByHatcheryId(hatcheryId);
    }

    public ChickBatch createChickBatch(ChickBatchRequest request) {
        ChickBatch cb = new ChickBatch();
        cb.setId(UUID.randomUUID());
        cb.setBatchCode(request.batchCode());
        cb.setHatcheryId(request.hatcheryId());
        cb.setHatchRecordId(request.hatchRecordId());
        cb.setHatchDate(request.hatchDate());
        cb.setGrade(request.grade());
        cb.setQtyAvailable(request.qtyAvailable());
        cb.setQtyBoxed(0);
        cb.setStatus(ChickBatch.Status.AVAILABLE);
        cb.setCreatedAt(Instant.now());
        cb.setUpdatedAt(Instant.now());
        return chickBatchRepository.save(cb);
    }

    public List<ChickBatch> listChickBatches(UUID hatcheryId) {
        return chickBatchRepository.findByHatcheryId(hatcheryId);
    }
}
