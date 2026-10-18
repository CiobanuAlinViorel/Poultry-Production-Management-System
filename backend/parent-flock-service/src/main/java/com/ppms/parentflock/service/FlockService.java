package com.ppms.parentflock.service;

import com.ppms.parentflock.domain.Flock;
import com.ppms.parentflock.domain.House;
import com.ppms.parentflock.dtos.CreateFlockRequest;
import com.ppms.parentflock.dtos.FlockResponse;
import com.ppms.parentflock.repository.BreederSiteRepository;
import com.ppms.parentflock.repository.FlockRepository;
import com.ppms.parentflock.repository.HouseRepository;
import com.ppms.shared.events.DomainEvent;
import com.ppms.shared.events.OutboxPublisher;
import com.ppms.shared.events.ParentFlockFlockPayload;
import com.ppms.shared.events.Topics;
import com.ppms.shared.exception.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class FlockService {

    private final FlockRepository flockRepository;
    private final BreederSiteRepository breederSiteRepository;
    private final HouseRepository houseRepository;
    private final OutboxPublisher outboxPublisher;

    public FlockService(FlockRepository flockRepository,
                        BreederSiteRepository breederSiteRepository,
                        HouseRepository houseRepository,
                        OutboxPublisher outboxPublisher) {
        this.flockRepository = flockRepository;
        this.breederSiteRepository = breederSiteRepository;
        this.houseRepository = houseRepository;
        this.outboxPublisher = outboxPublisher;
    }

    public FlockResponse create(UUID siteId, CreateFlockRequest request) {
        if (!breederSiteRepository.existsById(siteId)) {
            throw new EntityNotFoundException("BreederSite", siteId);
        }
        House house = houseRepository.findById(request.houseId())
                .orElseThrow(() -> new EntityNotFoundException("House", request.houseId()));
        if (!house.getSiteId().equals(siteId)) {
            throw new EntityNotFoundException("House", request.houseId());
        }
        if (flockRepository.existsByHouseIdAndStatus(request.houseId(), Flock.FlockStatus.ACTIVE)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "House already has an active flock");
        }
        int birds = request.femaleCount() + request.maleCount();
        if (birds <= 0) {
            throw new IllegalArgumentException("Flock must contain at least one bird");
        }
        if (birds > house.getCapacity()) {
            throw new IllegalArgumentException("House capacity exceeded");
        }
        Flock flock = new Flock();
        flock.setSiteId(siteId);
        flock.setHouseId(request.houseId());
        flock.setBreedCode(request.breedCode());
        flock.setArrivalDate(request.arrivalDate());
        flock.setExpectedEndDate(request.expectedEndDate());
        flock.setFemaleCount(request.femaleCount());
        flock.setMaleCount(request.maleCount());
        Flock saved = flockRepository.save(flock);
        outboxPublisher.publish(Topics.PARENT_FLOCK_FLOCK, new DomainEvent<>(UUID.randomUUID(), "parent-flock.flock.created", 1, "Flock", saved.getId(), saved.getSiteId(), null,
                new ParentFlockFlockPayload(saved.getId(), saved.getSiteId(), saved.getHouseId(), saved.getBreedCode(), saved.getArrivalDate(), saved.getExpectedEndDate(), saved.getFemaleCount(), saved.getMaleCount(), saved.getStatus().name())));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<FlockResponse> listBySite(UUID siteId) {
        if (!breederSiteRepository.existsById(siteId)) {
            throw new EntityNotFoundException("BreederSite", siteId);
        }
        return flockRepository.findBySiteId(siteId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FlockResponse> listBySiteAndHouse(UUID siteId, UUID houseId) {
        if (!breederSiteRepository.existsById(siteId)) {
            throw new EntityNotFoundException("BreederSite", siteId);
        }
        if (!houseRepository.existsById(houseId)) {
            throw new EntityNotFoundException("House", houseId);
        }
        return flockRepository.findBySiteIdAndHouseId(siteId, houseId).stream()
                .map(this::toResponse)
                .toList();
    }

    public FlockResponse close(UUID id) {
        Flock flock = flockRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Flock", id));
        flock.setStatus(Flock.FlockStatus.CLOSED);
        return toResponse(flockRepository.save(flock));
    }

    private FlockResponse toResponse(Flock flock) {
        return new FlockResponse(
                flock.getId(),
                flock.getSiteId(),
                flock.getHouseId(),
                flock.getBreedCode(),
                flock.getArrivalDate(),
                flock.getExpectedEndDate(),
                flock.getStatus(),
                flock.getFemaleCount(),
                flock.getMaleCount(),
                flock.getCreatedAt(),
                flock.getUpdatedAt()
        );
    }
}
