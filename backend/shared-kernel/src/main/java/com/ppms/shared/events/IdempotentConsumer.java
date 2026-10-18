package com.ppms.shared.events;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class IdempotentConsumer {

    private final ProcessedEventRepository processedEventRepository;

    @Transactional
    public void process(UUID eventId, String consumer, Runnable handler) {
        if (processedEventRepository.existsByEventIdAndConsumer(eventId, consumer)) {
            return;
        }
        handler.run();
        ProcessedEventEntity record = new ProcessedEventEntity();
        record.setEventId(eventId);
        record.setConsumer(consumer);
        record.setProcessedAt(Instant.now());
        processedEventRepository.save(record);
    }
}
