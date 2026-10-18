package com.example.identity_service.services;

import com.example.identity_service.domain.*;
import com.example.identity_service.dtos.*;
import com.example.identity_service.repositories.*;
import com.ppms.shared.audit.Audited;
import com.ppms.shared.events.DomainEvent;
import com.ppms.shared.events.IdentityEventPayload;
import com.ppms.shared.events.OutboxPublisher;
import com.ppms.shared.events.Topics;
import com.ppms.shared.security.UserPrincipal;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final CredentialRepository credentialRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ActivationTokenRepository activationTokenRepository;
    private final PasswordPolicyService passwordPolicyService;
    private final TokenService tokenService;
    private final OutboxPublisher outboxPublisher;

    @Audited(action = "USER_CREATE", entity = "User")
    public UserResponse createUser(CreateUserRequest request, UUID createdBy) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already in use");
        }
        if (request.username() != null && userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Username already in use");
        }
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(request.email());
        user.setUsername(request.username());
        user.setDisplayName(request.displayName());
        user.setEmployeeId(request.employeeId());
        user.setLocale(request.locale() != null ? request.locale() : "ro");
        user.setStatus(UserStatus.PENDING_ACTIVATION);
        user.setFailedLoginAttempts(0);
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        Credential credential = new Credential();
        credential.setUserId(user.getId());
        credential.setPasswordHash(passwordPolicyService.hash(UUID.randomUUID().toString()));
        credential.setMustChangePassword(true);
        credential.setPasswordHistory(new ArrayList<>());
        credential.setMfaRecoveryCodes(new ArrayList<>());
        credential.setCreatedAt(Instant.now());
        credential.setUpdatedAt(Instant.now());
        credentialRepository.save(credential);

        if (request.sendActivationEmail()) {
            RawAndHashedToken token = tokenService.generateRefreshToken();
            ActivationToken activation = new ActivationToken(token.hashed(), user.getId(), Instant.now().plus(7, ChronoUnit.DAYS), null);
            activationTokenRepository.save(activation);
            outboxPublisher.publish(Topics.IDENTITY_USER, new DomainEvent<>(UUID.randomUUID(), "identity.user.activation-requested", 1, "User", user.getId(), null, createdBy,
                    new IdentityEventPayload(user.getId(), user.getEmail(), user.getUsername(), user.getDisplayName(), token.raw(), "activation")));
        }

        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> listUsers(UserStatus status, String q, Pageable pageable) {
        return userRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(UUID id) {
        return toResponse(userRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("User not found")));
    }

    public UserResponse updateUser(UUID id, UpdateProfileRequest request) {
        User user = userRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("User not found"));
        if (request.displayName() != null) user.setDisplayName(request.displayName());
        if (request.phone() != null) user.setPhone(request.phone());
        if (request.locale() != null) user.setLocale(request.locale());
        if (request.avatarUrl() != null) user.setAvatarUrl(request.avatarUrl());
        user.setUpdatedAt(Instant.now());
        return toResponse(userRepository.save(user));
    }

    @Audited(action = "USER_DISABLE", entity = "User", idExpression = "#id")
    public void disableUser(UUID id) {
        User user = userRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("User not found"));
        user.setStatus(UserStatus.DISABLED);
        userRepository.save(user);
        logoutAll(id);
    }

    @Audited(action = "USER_ENABLE", entity = "User", idExpression = "#id")
    public void enableUser(UUID id) {
        User user = userRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("User not found"));
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
    }

    @Audited(action = "USER_UNLOCK", entity = "User", idExpression = "#id")
    public void unlockUser(UUID id) {
        User user = userRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("User not found"));
        user.setStatus(UserStatus.ACTIVE);
        user.setFailedLoginAttempts(0);
        user.setLockedAt(null);
        userRepository.save(user);
    }

    @Audited(action = "USER_RESET_PASSWORD", entity = "User", idExpression = "#id")
    public void adminResetPassword(UUID id) {
        User user = userRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("User not found"));
        Credential credential = credentialRepository.findByUserId(id).orElseThrow();
        credential.setMustChangePassword(true);
        credentialRepository.save(credential);
    }

    @Audited(action = "USER_FORCE_PASSWORD_CHANGE", entity = "User", idExpression = "#id")
    public void forcePasswordChange(UUID id) {
        adminResetPassword(id);
    }

    @Audited(action = "USER_MFA_RESET", entity = "User", idExpression = "#id")
    public void resetMfa(UUID id) {
        Credential credential = credentialRepository.findByUserId(id).orElseThrow();
        credential.setMfaEnabled(false);
        credential.setMfaSecretEnc(null);
        credential.setMfaRecoveryCodes(new ArrayList<>());
        credentialRepository.save(credential);
    }

    public void logoutAll(UUID userId) {
        refreshTokenRepository.findByUserIdAndRevokedAtIsNull(userId)
                .forEach(t -> t.setRevokedAt(Instant.now()));
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(), user.getEmail(), user.getUsername(), user.getDisplayName(),
                user.getPhone(), user.getLocale(), user.getStatus(), user.getEmployeeId(),
                user.getLastLoginAt(), user.getCreatedAt()
        );
    }
}
