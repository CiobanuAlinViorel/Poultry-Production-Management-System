package com.ppms.notification.controllers;

import com.ppms.notification.domain.Notification;
import com.ppms.notification.domain.UserPreference;
import com.ppms.notification.services.NotificationService;
import com.ppms.shared.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/v1/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final Map<UUID, SseEmitter> emitters = new ConcurrentHashMap<>();

    @GetMapping("/notifications")
    public ResponseEntity<List<Notification>> list(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(notificationService.listNotifications(principal.getUserId()));
    }

    @PostMapping("/notifications/{id}/read")
    public ResponseEntity<Void> markRead(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID id) {
        notificationService.markRead(principal.getUserId(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/preferences")
    public ResponseEntity<UserPreference> getPreference(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(notificationService.getOrCreatePreference(principal.getUserId()));
    }

    @PutMapping("/preferences")
    public ResponseEntity<UserPreference> updatePreference(@AuthenticationPrincipal UserPrincipal principal, @RequestBody UserPreference update) {
        return ResponseEntity.ok(notificationService.updatePreference(principal.getUserId(), update));
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@AuthenticationPrincipal UserPrincipal principal) {
        SseEmitter emitter = new SseEmitter(0L);
        UUID userId = principal.getUserId();
        emitters.put(userId, emitter);
        emitter.onCompletion(() -> emitters.remove(userId));
        emitter.onTimeout(() -> emitters.remove(userId));
        emitter.onError(e -> emitters.remove(userId));
        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException ignored) {
        }
        return emitter;
    }

    public void sendToUser(UUID userId, Object payload) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().name("notification").data(payload));
            } catch (IOException e) {
                emitters.remove(userId);
            }
        }
    }
}
