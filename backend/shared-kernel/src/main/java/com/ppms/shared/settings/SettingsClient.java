package com.ppms.shared.settings;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.ppms.shared.domain.ScopeType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Component
public class SettingsClient {

    private final RestClient restClient;
    private final com.github.benmanes.caffeine.cache.Cache<String, Object> cache;

    public SettingsClient(@Value("${ppms.admin-service.url:http://localhost:8085}") String adminUrl) {
        this.restClient = RestClient.builder().baseUrl(adminUrl).build();
        this.cache = Caffeine.newBuilder().expireAfterWrite(Duration.ofMinutes(10)).build();
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, ScopeType scopeType, UUID scopeId, T defaultValue) {
        String cacheKey = key + "|" + scopeType + "|" + scopeId;
        Object cached = cache.getIfPresent(cacheKey);
        if (cached != null) {
            return (T) cached;
        }
        try {
            String url = "/api/v1/settings/" + key + "?scopeType=" + scopeType + (scopeId != null ? "&scopeId=" + scopeId : "");
            T value = restClient.get().uri(url).retrieve().body((Class<T>) defaultValue.getClass());
            if (value != null) {
                cache.put(cacheKey, value);
                return value;
            }
        } catch (RestClientException e) {
            log.warn("Admin service settings unreachable for key {}, using default", key);
        }
        return defaultValue;
    }

    @CacheEvict(value = "settings", allEntries = true)
    public void invalidate() {
        cache.invalidateAll();
    }
}
