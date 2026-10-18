package com.ppms.shared.persistence;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;

/**
 * Generates UUIDv7 (time-ordered UUID) for index locality.
 * Layout: 48 bits unix-ts-ms + 4 bits version (0111) + 62 random bits.
 */
public final class UuidV7Generator {

    private static final SecureRandom RANDOM = new SecureRandom();

    private UuidV7Generator() {
    }

    public static UUID generate() {
        return generate(Instant.now());
    }

    public static UUID generate(Instant instant) {
        long ts = instant.toEpochMilli();

        byte[] rand = new byte[10];
        RANDOM.nextBytes(rand);

        long msb = (ts << 16) | ((rand[0] & 0xFFL) << 8) | (rand[1] & 0xFFL);
        msb = (msb & 0xFFFFFFFFFFFF0FFFL) | 0x0000000000007000L;

        long lsb = 0;
        for (int i = 2; i < 10; i++) {
            lsb = (lsb << 8) | (rand[i] & 0xFFL);
        }
        lsb = (lsb & 0x3FFFFFFFFFFFFFFFL) | 0x8000000000000000L;

        return new UUID(msb, lsb);
    }
}
