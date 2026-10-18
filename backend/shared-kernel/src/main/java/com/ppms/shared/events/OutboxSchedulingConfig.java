package com.ppms.shared.events;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables scheduling in every service that scans {@code com.ppms.shared}, so the
 * {@link OutboxRelay} actually runs. Without this the outbox fills up but nothing is
 * ever published to Kafka (idempotent if a service also declares @EnableScheduling).
 */
@Configuration
@EnableScheduling
public class OutboxSchedulingConfig {
}
