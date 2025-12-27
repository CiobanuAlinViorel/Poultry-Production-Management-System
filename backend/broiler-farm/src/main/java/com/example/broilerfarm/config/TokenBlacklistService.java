package com.example.broilerfarm.config;


import com.example.broilerfarm.services.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Token Blacklist Service - In-Memory Implementation
 *
 * Simplu, fără dependințe externe (Redis/DB)
 * Perfect pentru proiecte de facultate sau aplicații mici
 *
 * IMPORTANT: Blacklist-ul se șterge la restart aplicație!
 * Pentru producție, folosește Redis.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistService {

    private final JwtService jwtService;

    // Map<token, expirationDate>
    // ConcurrentHashMap pentru thread-safety
    private final Map<String, Date> blacklistedTokens = new ConcurrentHashMap<>();

    /**
     * Adaugă un token în blacklist
     *
     * @param token - token-ul JWT de invalidat
     */
    public void blacklistToken(String token) {
        try {
            // Extrage data de expirare din token
            Date expirationDate = jwtService.extractExpiration(token);

            // Salvează în blacklist cu data de expirare
            blacklistedTokens.put(token, expirationDate);

            log.info("Token blacklisted. Total blacklisted tokens: {}", blacklistedTokens.size());

        } catch (Exception e) {
            log.error("Error blacklisting token: {}", e.getMessage());
        }
    }

    /**
     * Verifică dacă un token e în blacklist
     *
     * @param token - token-ul de verificat
     * @return true dacă token-ul e invalidat
     */
    public boolean isBlacklisted(String token) {
        return blacklistedTokens.containsKey(token);
    }

    /**
     * Cleanup automat al token-urilor expirate din blacklist
     *
     * Se execută la fiecare oră și șterge token-urile care au expirat oricum
     * (nu mai are sens să le ținem în memorie)
     */
    @Scheduled(fixedRate = 3600000) // La fiecare oră
    public void cleanupExpiredTokens() {
        Date now = new Date();
        int initialSize = blacklistedTokens.size();

        // Șterge token-urile care au expirat
        blacklistedTokens.entrySet().removeIf(entry -> entry.getValue().before(now));

        int removedCount = initialSize - blacklistedTokens.size();
        if (removedCount > 0) {
            log.info("Cleaned up {} expired tokens from blacklist. Remaining: {}",
                    removedCount, blacklistedTokens.size());
        }
    }

    /**
     * Șterge toate token-urile din blacklist
     * Util pentru testing
     */
    public void clearAll() {
        blacklistedTokens.clear();
        log.info("Blacklist cleared");
    }

    /**
     * Returnează numărul de token-uri în blacklist
     */
    public int getBlacklistSize() {
        return blacklistedTokens.size();
    }
}