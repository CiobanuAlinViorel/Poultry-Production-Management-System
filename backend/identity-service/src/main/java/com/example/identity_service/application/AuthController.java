package com.example.identity_service.application;

import com.example.identity_service.dtos.*;
import com.example.identity_service.services.AuthService;
import com.ppms.shared.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(
            @RequestBody @Valid LoginRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.login(request, httpRequest.getHeader("User-Agent"), extractIpAddress(httpRequest)));
    }

    @PostMapping("/mfa/verify")
    public ResponseEntity<TokenResponse> verifyMfa(
            @RequestBody @Valid MfaVerifyRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.verifyMfa(request, httpRequest.getHeader("User-Agent"), extractIpAddress(httpRequest)));
    }

    @PostMapping("/pin-login")
    public ResponseEntity<TokenResponse> pinLogin(
            @RequestBody @Valid PinLoginRequest request,
            @RequestHeader("X-Device-Token") String deviceToken,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.pinLogin(request, deviceToken, httpRequest.getHeader("User-Agent"), extractIpAddress(httpRequest)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@RequestBody @Valid RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody @Valid LogoutRequest request) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll(@AuthenticationPrincipal UserPrincipal principal) {
        authService.logoutAll(principal.getUserId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<AuthService.UserProfileDto> me(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(authService.me(principal.getUserId()));
    }

    @PatchMapping("/me")
    public ResponseEntity<AuthService.UserProfileDto> updateMe(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(authService.updateMe(principal.getUserId(), request));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @RequestBody @Valid ChangePasswordRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        authService.changePassword(principal.getUserId(), request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(
            @RequestParam String email,
            HttpServletRequest httpRequest) {
        authService.forgotPassword(email, extractIpAddress(httpRequest));
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        authService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/activate")
    public ResponseEntity<Void> activate(@RequestBody @Valid ActivationRequest request) {
        authService.activate(request.token(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/sessions")
    public ResponseEntity<java.util.List<AuthService.SessionDto>> sessions(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(authService.sessions(principal.getUserId()));
    }

    @DeleteMapping("/sessions/{familyId}")
    public ResponseEntity<Void> revokeSession(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID familyId) {
        authService.revokeSession(principal.getUserId(), familyId);
        return ResponseEntity.noContent().build();
    }

    private String extractIpAddress(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
