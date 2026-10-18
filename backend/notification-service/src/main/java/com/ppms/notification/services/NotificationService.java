package com.ppms.notification.services;

import com.ppms.notification.domain.Alert;
import com.ppms.notification.domain.Notification;
import com.ppms.notification.domain.UserPreference;
import com.ppms.notification.repositories.AlertRepository;
import com.ppms.notification.repositories.NotificationRepository;
import com.ppms.notification.repositories.UserPreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final AlertRepository alertRepository;
    private final NotificationRepository notificationRepository;
    private final UserPreferenceRepository userPreferenceRepository;

    public Alert ingestAlert(String alertCode, Alert.Severity severity, String titleKey, String messageKey,
                             String dedupKey, UUID scopeId, String scopeType, String entityType, UUID entityId) {
        return alertRepository.findByDedupKeyAndStatus(dedupKey, Alert.Status.OPEN)
                .map(alert -> {
                    alert.setOccurrences(alert.getOccurrences() + 1);
                    alert.setLastSeenAt(Instant.now());
                    if (severity.ordinal() > alert.getSeverity().ordinal()) {
                        alert.setSeverity(severity);
                    }
                    return alertRepository.save(alert);
                })
                .orElseGet(() -> {
                    Alert alert = new Alert();
                    alert.setId(UUID.randomUUID());
                    alert.setAlertCode(alertCode);
                    alert.setSeverity(severity);
                    alert.setTitleKey(titleKey);
                    alert.setMessageKey(messageKey);
                    alert.setDedupKey(dedupKey);
                    alert.setScopeId(scopeId);
                    alert.setScopeType(scopeType);
                    alert.setEntityType(entityType);
                    alert.setEntityId(entityId);
                    alert.setStatus(Alert.Status.OPEN);
                    alert.setRaisedAt(Instant.now());
                    alert.setLastSeenAt(Instant.now());
                    alert.setCreatedAt(Instant.now());
                    alert.setUpdatedAt(Instant.now());
                    return alertRepository.save(alert);
                });
    }

    @Transactional(readOnly = true)
    public List<Notification> listNotifications(UUID userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public void markRead(UUID userId, UUID notificationId) {
        Notification n = notificationRepository.findById(notificationId).orElseThrow();
        if (!n.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Not owner");
        }
        n.setRead(true);
        n.setReadAt(Instant.now());
        notificationRepository.save(n);
    }

    public UserPreference getOrCreatePreference(UUID userId) {
        return userPreferenceRepository.findById(userId).orElseGet(() -> {
            UserPreference p = new UserPreference();
            p.setUserId(userId);
            p.setLocale("ro");
            p.setCreatedAt(Instant.now());
            p.setUpdatedAt(Instant.now());
            return userPreferenceRepository.save(p);
        });
    }

    public UserPreference updatePreference(UUID userId, UserPreference update) {
        UserPreference p = getOrCreatePreference(userId);
        p.setLocale(update.getLocale());
        p.setEmailEnabled(update.isEmailEnabled());
        p.setPushEnabled(update.isPushEnabled());
        p.setDigest(update.getDigest());
        p.setUpdatedAt(Instant.now());
        return userPreferenceRepository.save(p);
    }
}
