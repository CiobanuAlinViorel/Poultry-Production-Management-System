package com.example.shared.infrastructure.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenBlacklistService {

    private final Map<String, Instant> blacklistedTokens = new ConcurrentHashMap<>();

    public void blacklistToken(String token, Instant expiry) {
        blacklistedTokens.put(token, expiry);
    }

    public void blacklistToken(String token) {
        // Default expiry 24 hours from now
        blacklistToken(token, Instant.now().plusSeconds(24 * 60 * 60));
    }

    public boolean isTokenBlacklisted(String token) {
        Instant expiry = blacklistedTokens.get(token);
        if (expiry == null) {
            return false;
        }

        if (expiry.isBefore(Instant.now())) {
            // Token expired from blacklist, remove it
            blacklistedTokens.remove(token);
            return false;
        }

        return true;
    }

    public void removeFromBlacklist(String token) {
        blacklistedTokens.remove(token);
    }

    @Scheduled(fixedRate = 3600000) // Run every hour
    public void cleanupExpiredTokens() {
        Instant now = Instant.now();
        blacklistedTokens.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
    }

    public int getBlacklistSize() {
        return blacklistedTokens.size();
    }
}