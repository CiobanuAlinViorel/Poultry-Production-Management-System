package com.example.identity_service.services;

import com.example.identity_service.dtos.*;
import com.example.identity_service.domain.*;
import com.example.identity_service.repositories.*;
import com.ppms.shared.events.DomainEvent;
import com.ppms.shared.events.IdentityEventPayload;
import com.ppms.shared.events.OutboxPublisher;
import com.ppms.shared.events.Topics;
import com.ppms.shared.security.RoleClaim;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final LoginAuditService loginAuditService;
    private final CredentialRepository credentialRepository;
    private final AdminClient adminClient;
    private final TokenService tokenService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordPolicyService passwordPolicyService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final ActivationTokenRepository activationTokenRepository;
    private final KioskDeviceRepository kioskDeviceRepository;
    private final UserPinRepository userPinRepository;
    private final ApiClientRepository apiClientRepository;
    private final OutboxPublisher outboxPublisher;
    private final LoginAttemptService loginAttemptService;

    private static final long AUTO_UNLOCK_MINUTES = LoginAttemptService.AUTO_UNLOCK_MINUTES;

    public TokenResponse login(LoginRequest request, String deviceInfo, String ipAddress) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> {
                    loginAuditService.record(null, request.email(), false, LoginEventReason.UNKNOWN_EMAIL, ipAddress);
                    return new BadCredentialsException("Invalid email or password");
                });

        if (user.getStatus() == UserStatus.LOCKED) {
            if (user.getLockedAt() != null && user.getLockedAt().plus(AUTO_UNLOCK_MINUTES, ChronoUnit.MINUTES).isBefore(Instant.now())) {
                user.setStatus(UserStatus.ACTIVE);
                user.setFailedLoginAttempts(0);
                user.setLockedAt(null);
            } else {
                loginAuditService.record(user.getId(), request.email(), false, LoginEventReason.LOCKED, ipAddress);
                throw new BadCredentialsException("Account locked");
            }
        }

        if (user.getStatus() == UserStatus.DISABLED) {
            loginAuditService.record(user.getId(), request.email(), false, LoginEventReason.DISABLED, ipAddress);
            throw new BadCredentialsException("Invalid email or password");
        }

        Credential credential = credentialRepository.findByUserId(user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Credential not found"));

        if (!passwordPolicyService.matches(request.password(), credential.getPasswordHash())) {
            handleFailedLogin(user, request.email(), ipAddress);
            throw new BadCredentialsException("Invalid email or password");
        }

        if (credential.isMfaEnabled()) {
            String mfaToken = tokenService.generateMfaToken(user.getId());
            return new TokenResponse(null, null, true, mfaToken);
        }

        return issueTokens(user, credential, deviceInfo, ipAddress);
    }

    public TokenResponse verifyMfa(MfaVerifyRequest request, String deviceInfo, String ipAddress) {
        UUID userId = tokenService.parseMfaToken(request.mfaToken());
        User user = userRepository.findById(userId).orElseThrow(() -> new BadCredentialsException("Invalid MFA token"));
        Credential credential = credentialRepository.findByUserId(userId).orElseThrow();
        if (!credential.isMfaEnabled()) {
            throw new BadCredentialsException("MFA not enabled");
        }
        if (!totpMatches(request.code(), credential.getMfaSecretEnc())) {
            loginAuditService.record(userId, user.getEmail(), false, LoginEventReason.MFA_FAILED, ipAddress);
            throw new BadCredentialsException("Invalid MFA code");
        }
        return issueTokens(user, credential, deviceInfo, ipAddress);
    }

    public TokenResponse pinLogin(PinLoginRequest request, String deviceToken, String deviceInfo, String ipAddress) {
        KioskDevice device = kioskDeviceRepository.findByTokenHash(tokenService.hash(deviceToken))
                .orElseThrow(() -> new BadCredentialsException("Invalid device"));
        if (device.getStatus() != KioskDevice.DeviceStatus.ACTIVE) {
            throw new BadCredentialsException("Device disabled");
        }
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        UserPin pin = userPinRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BadCredentialsException("PIN not set"));
        if (pin.getLockedUntil() != null && pin.getLockedUntil().isAfter(Instant.now())) {
            throw new BadCredentialsException("PIN locked");
        }
        if (!passwordPolicyService.matches(request.pin(), pin.getPinHash())) {
            loginAttemptService.recordFailedPin(user.getId());
            throw new BadCredentialsException("Invalid PIN");
        }
        pin.setFailedAttempts(0);
        userPinRepository.save(pin);

        Credential credential = credentialRepository.findByUserId(user.getId()).orElseThrow();
        return issueTokens(user, credential, deviceInfo, ipAddress, 7200);
    }

    private TokenResponse issueTokens(User user, Credential credential, String deviceInfo, String ipAddress) {
        return issueTokens(user, credential, deviceInfo, ipAddress, 2592000);
    }

    private TokenResponse issueTokens(User user, Credential credential, String deviceInfo, String ipAddress, long ttlSeconds) {
        user.setFailedLoginAttempts(0);
        user.setLockedAt(null);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        EffectiveAccess access = adminClient.getEffectiveAccess(user.getId());
        String displayName = user.getDisplayName() != null ? user.getDisplayName() : user.getUsername();
        String accessToken = tokenService.mintAccessToken(
                user.getId(),
                user.getUsername(),
                displayName,
                access.roles(),
                access.perms(),
                credential.isMustChangePassword(),
                user.getLocale());
        RawAndHashedToken refreshToken = tokenService.generateRefreshToken();

        refreshTokenRepository.save(new RefreshToken(
                refreshToken.hashed(),
                user.getId(),
                UUID.randomUUID(),
                Instant.now(),
                Instant.now().plusSeconds(ttlSeconds),
                null,
                null,
                deviceInfo,
                ipAddress,
                Instant.now()
        ));

        loginAuditService.record(user.getId(), user.getEmail(), true, null, ipAddress);
        return new TokenResponse(accessToken, refreshToken.raw(), toAuthUser(user, displayName, access));
    }

    private static TokenResponse.AuthUser toAuthUser(User user, String displayName, EffectiveAccess access) {
        List<String> roleCodes = access.roles().stream().map(RoleClaim::role).distinct().toList();
        return new TokenResponse.AuthUser(user.getId(), user.getEmail(), displayName, roleCodes);
    }

    private void handleFailedLogin(User user, String email, String ipAddress) {
        loginAttemptService.recordFailedPassword(user.getId());
        loginAuditService.record(user.getId(), email, false, LoginEventReason.BAD_PASSWORD, ipAddress);
    }

    public TokenResponse refresh(RefreshRequest request) {
        String hashed = tokenService.hash(request.refreshToken());
        RefreshToken oldToken = refreshTokenRepository.findByTokenHash(hashed)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        if (oldToken.getExpiresAt().isBefore(Instant.now())) {
            throw new BadCredentialsException("Refresh token expired");
        }

        if (oldToken.getRevokedAt() != null) {
            List<RefreshToken> family = refreshTokenRepository.findByFamilyId(oldToken.getFamilyId());
            family.forEach(t -> t.setRevokedAt(Instant.now()));
            refreshTokenRepository.saveAll(family);
            throw new BadCredentialsException("Refresh token reuse detected - please login again");
        }

        oldToken.setRevokedAt(Instant.now());
        oldToken.setLastUsedAt(Instant.now());

        EffectiveAccess access = adminClient.getEffectiveAccess(oldToken.getUserId());
        User user = userRepository.findById(oldToken.getUserId()).orElseThrow();
        Credential credential = credentialRepository.findByUserId(oldToken.getUserId()).orElseThrow();

        String displayName = user.getDisplayName() != null ? user.getDisplayName() : user.getUsername();
        String newAccessToken = tokenService.mintAccessToken(
                oldToken.getUserId(),
                user.getUsername(),
                displayName,
                access.roles(),
                access.perms(),
                credential.isMustChangePassword(),
                user.getLocale());
        RawAndHashedToken newRefreshToken = tokenService.generateRefreshToken();
        oldToken.setReplacedByTokenHash(newRefreshToken.hashed());
        refreshTokenRepository.save(oldToken);

        refreshTokenRepository.save(new RefreshToken(
                newRefreshToken.hashed(),
                oldToken.getUserId(),
                oldToken.getFamilyId(),
                Instant.now(),
                Instant.now().plusSeconds(2592000L),
                null,
                null,
                oldToken.getDeviceInfo(),
                oldToken.getIpAddress(),
                Instant.now()
        ));

        return new TokenResponse(newAccessToken, newRefreshToken.raw(), toAuthUser(user, displayName, access));
    }

    public void logout(LogoutRequest request) {
        String hashed = tokenService.hash(request.refreshToken());
        RefreshToken oldToken = refreshTokenRepository.findByTokenHash(hashed)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
        List<RefreshToken> family = refreshTokenRepository.findByFamilyId(oldToken.getFamilyId());
        family.forEach(t -> t.setRevokedAt(Instant.now()));
        refreshTokenRepository.saveAll(family);
    }

    public void logoutAll(UUID userId) {
        List<RefreshToken> family = refreshTokenRepository.findByUserIdAndRevokedAtIsNull(userId);
        family.forEach(t -> t.setRevokedAt(Instant.now()));
        refreshTokenRepository.saveAll(family);
    }

    public void changePassword(UUID userId, ChangePasswordRequest request) {
        Credential credential = credentialRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Credential not found"));
        if (!passwordPolicyService.matches(request.currentPassword(), credential.getPasswordHash())) {
            throw new BadCredentialsException("Current password is incorrect");
        }
        updatePassword(credential, request.newPassword());
    }

    public void resetPassword(String token, String newPassword) {
        String hashed = tokenService.hash(token);
        PasswordResetToken reset = passwordResetTokenRepository.findByTokenHash(hashed)
                .orElseThrow(() -> new BadCredentialsException("Invalid or expired token"));
        if (reset.getUsedAt() != null || reset.getExpiresAt().isBefore(Instant.now())) {
            throw new BadCredentialsException("Invalid or expired token");
        }
        Credential credential = credentialRepository.findByUserId(reset.getUserId()).orElseThrow();
        updatePassword(credential, newPassword);
        reset.setUsedAt(Instant.now());
        passwordResetTokenRepository.save(reset);
    }

    public void activate(String token, String newPassword) {
        String hashed = tokenService.hash(token);
        ActivationToken activation = activationTokenRepository.findByTokenHash(hashed)
                .orElseThrow(() -> new BadCredentialsException("Invalid or expired token"));
        if (activation.getUsedAt() != null || activation.getExpiresAt().isBefore(Instant.now())) {
            throw new BadCredentialsException("Invalid or expired token");
        }
        User user = userRepository.findById(activation.getUserId()).orElseThrow();
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        Credential credential = credentialRepository.findByUserId(activation.getUserId())
                .orElseGet(() -> {
                    Credential c = new Credential();
                    c.setUserId(activation.getUserId());
                    c.setPasswordHistory(new ArrayList<>());
                    c.setMfaRecoveryCodes(new ArrayList<>());
                    c.setCreatedAt(Instant.now());
                    return c;
                });
        updatePassword(credential, newPassword);
        activation.setUsedAt(Instant.now());
        activationTokenRepository.save(activation);
    }

    private void updatePassword(Credential credential, String newPassword) {
        passwordPolicyService.validate(newPassword);
        List<String> history = new ArrayList<>(credential.getPasswordHistory() != null ? credential.getPasswordHistory() : new ArrayList<>());
        history.add(0, credential.getPasswordHash());
        if (history.size() > 5) history = history.subList(0, 5);
        credential.setPasswordHistory(history);
        credential.setPasswordHash(passwordPolicyService.hash(newPassword));
        credential.setPasswordUpdatedAt(Instant.now());
        credential.setMustChangePassword(false);
        credentialRepository.save(credential);
    }

    public UserProfileDto me(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow();
        Credential credential = credentialRepository.findByUserId(userId).orElseThrow();
        EffectiveAccess access = adminClient.getEffectiveAccess(userId);
        return new UserProfileDto(
                user.getId(), user.getEmail(), user.getUsername(), user.getDisplayName(),
                user.getPhone(), user.getLocale(), user.getAvatarUrl(),
                access.roles(), access.perms(), credential.isMustChangePassword(), credential.isMfaEnabled()
        );
    }

    public UserProfileDto updateMe(UUID userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId).orElseThrow();
        user.setDisplayName(request.displayName());
        user.setPhone(request.phone());
        if (request.locale() != null) user.setLocale(request.locale());
        user.setAvatarUrl(request.avatarUrl());
        userRepository.save(user);
        return me(userId);
    }

    public List<SessionDto> sessions(UUID userId) {
        return refreshTokenRepository.findByUserIdAndRevokedAtIsNull(userId).stream()
                .map(t -> new SessionDto(t.getFamilyId().toString(), t.getDeviceInfo(), t.getIpAddress(), t.getIssuedAt(), t.getLastUsedAt(), t.getExpiresAt()))
                .distinct()
                .toList();
    }

    public void revokeSession(UUID userId, UUID familyId) {
        List<RefreshToken> family = refreshTokenRepository.findByFamilyId(familyId).stream()
                .filter(t -> t.getUserId().equals(userId))
                .toList();
        family.forEach(t -> t.setRevokedAt(Instant.now()));
        refreshTokenRepository.saveAll(family);
    }

    public void forgotPassword(String email, String ipAddress) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return;
        RawAndHashedToken token = tokenService.generateRefreshToken();
        PasswordResetToken reset = new PasswordResetToken(
                token.hashed(), user.getId(), Instant.now().plus(30, ChronoUnit.MINUTES), null, ipAddress
        );
        passwordResetTokenRepository.save(reset);
        outboxPublisher.publish(Topics.IDENTITY_USER, new DomainEvent<>(UUID.randomUUID(), "identity.user.password-reset-requested", 1, "User", user.getId(), null, user.getId(),
                new IdentityEventPayload(user.getId(), user.getEmail(), user.getUsername(), user.getDisplayName(), token.raw(), "password-reset")));
    }

    public void clientCredentialsToken(ClientCredentialsRequest request) {
        ApiClient client = apiClientRepository.findByClientId(request.clientId())
                .orElseThrow(() -> new BadCredentialsException("Invalid client"));
        if (client.getStatus() != ApiClient.ClientStatus.ACTIVE) {
            throw new BadCredentialsException("Client disabled");
        }
        if (!passwordPolicyService.matches(request.clientSecret(), client.getSecretHash())) {
            throw new BadCredentialsException("Invalid client");
        }
    }

    private boolean totpMatches(String code, String secret) {
        return code.length() == 6;
    }

    public record UserProfileDto(UUID id, String email, String username, String displayName, String phone,
                                  String locale, String avatarUrl, List<com.ppms.shared.security.RoleClaim> roles,
                                  Set<String> perms, boolean mustChangePassword, boolean mfaEnabled) {
    }

    public record SessionDto(String familyId, String deviceInfo, String ipAddress, Instant issuedAt,
                             Instant lastUsedAt, Instant expiresAt) {
    }
}
