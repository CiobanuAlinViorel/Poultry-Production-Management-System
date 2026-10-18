package com.ppms.shared.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

public final class InternalCallSigner {

    public static final String TOKEN_HEADER = "X-Internal-Token";
    public static final String TIMESTAMP_HEADER = "X-Internal-Timestamp";
    public static final String SECRET_ENV = "PPMS_INTERNAL_SECRET";

    private InternalCallSigner() {
    }

    public static String currentTimestamp() {
        return Long.toString(Instant.now().getEpochSecond());
    }

    public static String sign(String secret, String timestamp) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] sig = mac.doFinal((timestamp + ":" + secret).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(sig);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to sign internal call", e);
        }
    }
}
