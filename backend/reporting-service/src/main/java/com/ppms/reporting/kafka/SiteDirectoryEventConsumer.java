package com.ppms.reporting.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ppms.reporting.domain.SiteOverview;
import com.ppms.reporting.repositories.SiteOverviewRepository;
import com.ppms.shared.events.*;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SiteDirectoryEventConsumer {

    private final SiteOverviewRepository siteOverviewRepository;
    private final ObjectMapper objectMapper;
    private final IdempotentConsumer idempotentConsumer;

    @KafkaListener(topics = Topics.SITE_DIRECTORY, groupId = "reporting-service-site-directory")
    @Transactional
    public void consume(EventEnvelope envelope) {
        DomainEvent<SiteDirectoryPayload> event = objectMapper.convertValue(envelope.payload(), objectMapper.getTypeFactory().constructParametricType(DomainEvent.class, SiteDirectoryPayload.class));
        idempotentConsumer.process(envelope.eventId(), "reporting-service-site-directory", () -> process(event));
    }

    private void process(DomainEvent<SiteDirectoryPayload> event) {
        SiteDirectoryPayload payload = event.payload();
        if (payload == null || payload.siteId() == null) {
            return;
        }
        UUID siteId = payload.siteId();
        SiteOverview overview = siteOverviewRepository.findById(siteId).orElseGet(() -> {
            SiteOverview s = new SiteOverview();
            s.setSiteId(siteId);
            s.setActiveUnits(0);
            s.setBirds(0);
            s.setOpenCriticalAlerts(0);
            s.setOpenWarningAlerts(0);
            return s;
        });
        overview.setSiteType(payload.siteType());
        overview.setName(payload.name());
        overview.setUpdatedAt(Instant.now());
        overview.setLastActivityAt(Instant.now());
        siteOverviewRepository.save(overview);
    }
}
