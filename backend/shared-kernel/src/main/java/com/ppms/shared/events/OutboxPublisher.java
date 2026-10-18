package com.ppms.shared.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ppms.shared.persistence.UuidV7Generator;
import com.ppms.shared.web.CorrelationId;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Value("${spring.application.name:ppms-service}")
    private String producer;

    @Transactional
    public <T> void publish(String topic, DomainEvent<T> event) {
        OutboxEntity outbox = new OutboxEntity();
        outbox.setId(UuidV7Generator.generate());
        outbox.setTopic(topic);
        outbox.setKey(event.aggregateId() != null ? event.aggregateId().toString() : event.eventId().toString());
        try {
            outbox.setHeaders(objectMapper.writeValueAsString(Map.of(
                    "eventType", event.eventType(),
                    "version", String.valueOf(event.version())
            )));
            java.util.UUID correlation = CorrelationId.get() != null ? java.util.UUID.fromString(CorrelationId.get()) : null;
            EventEnvelope<T> envelope = new EventEnvelope<>(
                    event.eventId(),
                    event.eventType(),
                    event.version(),
                    Instant.now(),
                    producer,
                    event.aggregateType(),
                    event.aggregateId(),
                    event.siteId(),
                    event.actorId(),
                    correlation,
                    null,
                    event.payload()
            );
            outbox.setPayload(objectMapper.writeValueAsString(envelope));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize domain event", e);
        }
        outbox.setCreatedAt(Instant.now());
        outboxRepository.save(outbox);
    }
}
