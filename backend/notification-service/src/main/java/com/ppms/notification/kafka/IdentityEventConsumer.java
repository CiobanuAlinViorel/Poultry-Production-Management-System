package com.ppms.notification.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ppms.notification.services.EmailService;
import com.ppms.shared.events.DomainEvent;
import com.ppms.shared.events.EventEnvelope;
import com.ppms.shared.events.IdempotentConsumer;
import com.ppms.shared.events.IdentityEventPayload;
import com.ppms.shared.events.Topics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdentityEventConsumer {

    private final EmailService emailService;
    private final ObjectMapper objectMapper;
    private final IdempotentConsumer idempotentConsumer;

    @KafkaListener(topics = Topics.IDENTITY_USER, groupId = "notification-service-identity")
    public void consume(EventEnvelope envelope) {
        DomainEvent<IdentityEventPayload> event = objectMapper.convertValue(envelope.payload(), objectMapper.getTypeFactory().constructParametricType(DomainEvent.class, IdentityEventPayload.class));
        idempotentConsumer.process(envelope.eventId(), "notification-service-identity", () -> process(event));
    }

    private void process(DomainEvent<IdentityEventPayload> event) {
        IdentityEventPayload payload = event.payload();
        if (payload == null || payload.email() == null) {
            return;
        }
        Context ctx = new Context();
        ctx.setVariable("name", payload.displayName() != null ? payload.displayName() : payload.username());
        ctx.setVariable("code", payload.token());
        switch (event.eventType()) {
            case "identity.user.password-reset-requested" ->
                    emailService.enqueue(payload.email(), "Resetare parolă PPMS", "password-reset", ctx);
            case "identity.user.activation-requested" ->
                    emailService.enqueue(payload.email(), "Activare cont PPMS", "activation", ctx);
            default -> log.debug("Unhandled identity event type: {}", event.eventType());
        }
    }
}
