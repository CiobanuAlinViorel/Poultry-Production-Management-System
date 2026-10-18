package com.ppms.admin.events;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ppms.admin.domain.SiteDirectoryEntry;
import com.ppms.admin.repositories.SiteDirectoryEntryRepository;
import com.ppms.shared.events.DomainEvent;
import com.ppms.shared.events.EventEnvelope;
import com.ppms.shared.events.IdempotentConsumer;
import com.ppms.shared.events.Topics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class SiteDirectoryConsumer {

    private final IdempotentConsumer idempotentConsumer;
    private final SiteDirectoryEntryRepository siteDirectoryRepository;
    private final ObjectMapper objectMapper;

    private static final String CONSUMER_NAME = "admin-site-directory";

    @KafkaListener(topics = Topics.SITE_DIRECTORY, groupId = "admin-service-site-directory")
    @Transactional
    public void consume(ConsumerRecord<String, String> record) {
        try {
            EventEnvelope<SiteDirectoryEventPayload> envelope = objectMapper.readValue(
                    record.value(), new TypeReference<>() {
                    });

            UUID eventId = envelope.eventId();
            if (eventId == null) {
                log.warn("Skipping site directory event without eventId");
                return;
            }

            SiteDirectoryEventPayload payload = envelope.payload();
            if (payload == null || payload.siteId() == null) {
                log.warn("Skipping site directory event without payload/siteId");
                return;
            }

            idempotentConsumer.process(eventId, CONSUMER_NAME, () -> upsert(payload));
        } catch (Exception e) {
            log.error("Failed to process site directory event", e);
            throw new RuntimeException("Failed to process site directory event", e);
        }
    }

    private void upsert(SiteDirectoryEventPayload payload) {
        Optional<SiteDirectoryEntry> existing = siteDirectoryRepository.findBySiteId(payload.siteId());
        SiteDirectoryEntry entry = existing.orElseGet(SiteDirectoryEntry::new);
        if (existing.isEmpty()) {
            entry.setSiteId(payload.siteId());
        }
        entry.setSiteType(payload.siteType());
        entry.setCode(payload.code());
        entry.setName(payload.name());
        entry.setAddress(payload.address());
        entry.setCounty(payload.county());
        entry.setActive(payload.active());
        entry.setSourceService(payload.sourceService());
        entry.setLastEventAt(Optional.ofNullable(payload.lastEventAt()).orElse(Instant.now()));
        siteDirectoryRepository.save(entry);
    }
}
