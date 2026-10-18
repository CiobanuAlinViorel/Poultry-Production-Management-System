package com.ppms.notification.controllers;

import com.ppms.notification.domain.Notification;
import com.ppms.notification.domain.UserPreference;
import com.ppms.notification.services.NotificationService;
import com.ppms.shared.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

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
}
