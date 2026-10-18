package com.ppms.shared.testsupport;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

@Configuration
@Profile("test")
public class TestClock {

    private final MutableClock clock = new MutableClock();

    @Bean
    @Primary
    public Clock clock() {
        return clock;
    }

    public void set(Instant instant) {
        clock.set(instant);
    }

    public static class MutableClock extends Clock {
        private Instant instant = Instant.now();
        private final ZoneId zone = ZoneId.of("Europe/Bucharest");

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }

        public void set(Instant instant) {
            this.instant = instant;
        }
    }
}
