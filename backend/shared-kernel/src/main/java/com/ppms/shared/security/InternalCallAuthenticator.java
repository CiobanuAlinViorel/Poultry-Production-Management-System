package com.ppms.shared.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Component
public class InternalCallAuthenticator {

    public boolean validate(HttpServletRequest request) {
        String token = request.getHeader(InternalCallSigner.TOKEN_HEADER);
        String timestamp = request.getHeader(InternalCallSigner.TIMESTAMP_HEADER);
        String secret = System.getenv(InternalCallSigner.SECRET_ENV);
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
            String expected = InternalCallSigner.sign(secret, timestamp);
            return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false;
        }
    }
}
