package com.ppms.parentflock.service;

import com.ppms.parentflock.domain.BreederSite;
import com.ppms.parentflock.dtos.BreederSiteResponse;
import com.ppms.parentflock.dtos.CreateBreederSiteRequest;
import com.ppms.parentflock.repository.BreederSiteRepository;
import com.ppms.shared.events.DomainEvent;
import com.ppms.shared.events.OutboxPublisher;
import com.ppms.shared.events.SiteDirectoryPayload;
import com.ppms.shared.events.Topics;
import com.ppms.shared.exception.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class BreederSiteService {

    private final BreederSiteRepository breederSiteRepository;
    private final OutboxPublisher outboxPublisher;

    public BreederSiteService(BreederSiteRepository breederSiteRepository, OutboxPublisher outboxPublisher) {
        this.breederSiteRepository = breederSiteRepository;
        this.outboxPublisher = outboxPublisher;
    }

    public BreederSiteResponse create(CreateBreederSiteRequest request) {
        BreederSite site = new BreederSite();
        site.setCode(request.code());
        site.setName(request.name());
        site.setAddress(request.address());
        site.setCounty(request.county());
        site.setCapacityHouses(request.capacityHouses());
        BreederSite saved = breederSiteRepository.save(site);
        outboxPublisher.publish(Topics.SITE_DIRECTORY, new DomainEvent<>(UUID.randomUUID(), "site.directory.upserted", 1, "SiteDirectory", saved.getId(), saved.getId(), null,
                new SiteDirectoryPayload(saved.getId(), "BREEDER_SITE", saved.getCode(), saved.getName(), saved.getAddress(), saved.getCounty(), saved.isActive(), "parent-flock-service", Instant.now())));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public BreederSiteResponse get(UUID id) {
        BreederSite site = breederSiteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("BreederSite", id));
        return toResponse(site);
    }

    @Transactional(readOnly = true)
    public List<BreederSiteResponse> list() {
        return breederSiteRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public BreederSiteResponse update(UUID id, CreateBreederSiteRequest request) {
        BreederSite site = breederSiteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("BreederSite", id));
        site.setCode(request.code());
        site.setName(request.name());
        site.setAddress(request.address());
        site.setCounty(request.county());
        site.setCapacityHouses(request.capacityHouses());
        return toResponse(breederSiteRepository.save(site));
    }

    private BreederSiteResponse toResponse(BreederSite site) {
        return new BreederSiteResponse(
                site.getId(),
                site.getCode(),
                site.getName(),
                site.getAddress(),
                site.getCounty(),
                site.isActive(),
                site.getCapacityHouses(),
                site.getCreatedAt(),
                site.getUpdatedAt()
        );
    }
}
