package com.ppms.shared.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelay {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    @Value("${ppms.outbox.poll-interval-ms:500}")
    private long pollIntervalMs;

    @Value("${ppms.outbox.batch-size:100}")
    private int batchSize;

    @PostConstruct
    public void init() {
        Gauge.builder("ppms_outbox_pending", outboxRepository, r -> r.findPending(1).size())
                .description("Pending outbox events")
                .register(meterRegistry);
    }

    @Scheduled(fixedDelayString = "${ppms.outbox.poll-interval-ms:500}", timeUnit = TimeUnit.MILLISECONDS)
    @Transactional
    public void poll() {
        List<OutboxEntity> pending = outboxRepository.findPending(batchSize);
        for (OutboxEntity event : pending) {
            try {
                send(event);
                outboxRepository.markPublished(event.getId(), Instant.now());
            } catch (Exception e) {
                log.warn("Failed to relay outbox event {}: {}", event.getId(), e.getMessage());
                outboxRepository.markError(event.getId(), e.getMessage());
            }
        }
    }

    @Scheduled(fixedDelay = 86400000, timeUnit = TimeUnit.MILLISECONDS)
    @Transactional
    public void purge() {
        Instant cutoff = Instant.now().minus(7, ChronoUnit.DAYS);
        int deleted = outboxRepository.deletePublishedOlderThan(cutoff);
        log.info("Purged {} old outbox events", deleted);
    }

    private void send(OutboxEntity event) throws Exception {
        var headers = objectMapper.readValue(event.getHeaders(), java.util.Map.class);
        Message<String> message = MessageBuilder.withPayload(event.getPayload())
                .setHeader(KafkaHeaders.TOPIC, event.getTopic())
                .setHeader(KafkaHeaders.KEY, event.getKey())
                .setHeader("eventType", headers.get("eventType"))
                .setHeader("version", headers.get("version"))
                .build();
        kafkaTemplate.send(message).get(5, TimeUnit.SECONDS);
    }
}
