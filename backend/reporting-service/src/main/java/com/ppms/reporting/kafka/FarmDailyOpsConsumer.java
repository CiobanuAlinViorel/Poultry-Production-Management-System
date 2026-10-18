package com.ppms.reporting.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ppms.reporting.domain.KpiDaily;
import com.ppms.reporting.repositories.KpiDailyRepository;
import com.ppms.shared.events.*;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FarmDailyOpsConsumer {

    private final KpiDailyRepository kpiDailyRepository;
    private final ObjectMapper objectMapper;
    private final IdempotentConsumer idempotentConsumer;

    @KafkaListener(topics = Topics.FARM_DAILY_OPS, groupId = "reporting-service-farm-daily")
    @Transactional
    public void consume(EventEnvelope envelope) {
        DomainEvent<FarmDailySheetPayload> event = objectMapper.convertValue(envelope.payload(), objectMapper.getTypeFactory().constructParametricType(DomainEvent.class, FarmDailySheetPayload.class));
        idempotentConsumer.process(envelope.eventId(), "reporting-service-farm-daily", () -> process(event));
    }

    private void process(DomainEvent<FarmDailySheetPayload> event) {
        FarmDailySheetPayload payload = event.payload();
        if (payload == null || payload.lotId() == null || payload.date() == null) {
            return;
        }
        UUID unitId = payload.houseId() != null ? payload.houseId() : payload.lotId();
        updateMetric(payload.farmId(), unitId, payload.date(), "MORTALITY", (double) payload.mortality());
        if (payload.feedKg() != null) {
            updateMetric(payload.farmId(), unitId, payload.date(), "FEED_KG", payload.feedKg().doubleValue());
        }
        if (payload.waterLiters() != null) {
            updateMetric(payload.farmId(), unitId, payload.date(), "WATER_L", payload.waterLiters().doubleValue());
        }
        if (payload.avgWeightG() != null && payload.avgWeightG().doubleValue() > 0) {
            updateMetric(payload.farmId(), unitId, payload.date(), "AVG_WEIGHT_G", payload.avgWeightG().doubleValue());
        }
    }

    private void updateMetric(UUID siteId, UUID unitId, java.time.LocalDate date, String metric, Double delta) {
        KpiDaily kpi = kpiDailyRepository.findBySiteIdAndUnitIdAndDateAndMetric(siteId, unitId, date, metric).orElseGet(() -> {
            KpiDaily k = new KpiDaily();
            k.setSiteId(siteId);
            k.setUnitId(unitId);
            k.setDate(date);
            k.setMetric(metric);
            k.setValue(0.0);
            return k;
        });
        kpi.setValue(kpi.getValue() + delta);
        kpiDailyRepository.save(kpi);
    }
}
