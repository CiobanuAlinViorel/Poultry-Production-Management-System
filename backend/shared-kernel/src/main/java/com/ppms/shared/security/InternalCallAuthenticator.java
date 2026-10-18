package com.ppms.shared.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

@Component
public class InternalCallAuthenticator {

    public boolean validate(HttpServletRequest request) {
        String token = request.getHeader("X-Internal-Token");
        String timestamp = request.getHeader("X-Internal-Timestamp");
        String secret = System.getenv("PPMS_INTERNAL_SECRET");
        if (secret == null || secret.isBlank()) {
            return false;
        }
        if (token == null || timestamp == null) {
            return false;
        }
        try {
            long ts = Long.parseLong(timestamp);
            if (Math.abs(Instant.now().getEpochSecond() - ts) > 60) {
                return false;
            }
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] sig = mac.doFinal((timestamp + ":" + secret).getBytes(StandardCharsets.UTF_8));
            String expected = Base64.getEncoder().encodeToString(sig);
            return expected.equals(token);
        } catch (Exception e) {
            return false;
        }
    }
}
