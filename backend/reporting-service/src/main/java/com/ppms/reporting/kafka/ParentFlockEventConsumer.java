package com.ppms.reporting.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ppms.reporting.domain.LotFact;
import com.ppms.reporting.repositories.LotFactRepository;
import com.ppms.shared.events.*;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class ParentFlockEventConsumer {

    private final LotFactRepository lotFactRepository;
    private final ObjectMapper objectMapper;
    private final IdempotentConsumer idempotentConsumer;

    @KafkaListener(topics = Topics.PARENT_FLOCK_FLOCK, groupId = "reporting-service-parent-flock")
    @Transactional
    public void consume(EventEnvelope envelope) {
        DomainEvent<ParentFlockFlockPayload> event = objectMapper.convertValue(envelope.payload(), objectMapper.getTypeFactory().constructParametricType(DomainEvent.class, ParentFlockFlockPayload.class));
        idempotentConsumer.process(envelope.eventId(), "reporting-service-parent-flock", () -> process(event));
    }

    private void process(DomainEvent<ParentFlockFlockPayload> event) {
        ParentFlockFlockPayload payload = event.payload();
        if (payload == null || payload.flockId() == null) {
            return;
        }
        LotFact fact = lotFactRepository.findById(payload.flockId()).orElseGet(() -> {
            LotFact f = new LotFact();
            f.setLotId(payload.flockId());
            return f;
        });
        fact.setFarmId(payload.siteId());
        fact.setHouseId(payload.houseId());
        fact.setBreedCode(payload.breedCode());
        fact.setPlacementDate(payload.arrivalDate() != null ? payload.arrivalDate().atStartOfDay(java.time.ZoneId.of("Europe/Bucharest")).toInstant() : null);
        fact.setCloseDate(payload.expectedEndDate() != null ? payload.expectedEndDate().atStartOfDay(java.time.ZoneId.of("Europe/Bucharest")).toInstant() : null);
        int birds = payload.femaleCount() + payload.maleCount();
        fact.setInitialBirds(birds);
        fact.setCurrentBirds(birds);
        fact.setUpdatedAt(Instant.now());
        lotFactRepository.save(fact);
    }
}
