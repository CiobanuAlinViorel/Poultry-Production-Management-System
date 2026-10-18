package com.example.identity_service.services;

import com.example.identity_service.dtos.*;
import com.example.identity_service.domain.*;
import com.example.identity_service.repositories.CredentialRepository;
import com.example.identity_service.repositories.RefreshTokenRepository;
import com.example.identity_service.repositories.UserRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.Instant;
import java.util.List;
import java.util.UUID;

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

    // LOGIN
    // 1. find user by email → unknown email → log audit event → throw
    // 2. check status LOCKED/DISABLED → log audit event → throw
    // 3. verify password with bcrypt → wrong → increment failedLoginAttempts
    //                                        → if >= 5 set LOCKED
    //                                        → log audit event → throw
    // 4. reset failedLoginAttempts, update lastLoginAt
    // 5. call AdminClient to fetch roles
    // 6. mint access token (JWT) + refresh token (opaque)
    // 7. store refresh token hash in DB
    // 8. log audit event (success)
    // 9. return both tokens
    public TokenResponse login(LoginRequest request, String deviceInfo, String ipAddress){
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> {
                    loginAuditService.record(
                            null,
                            request.email(),
                            false,
                            LoginEventReason.UNKNOWN_EMAIL,
                            ipAddress
                    );
                    return new BadCredentialsException("Invalid email or password");
                });
        if(user.getStatus() == UserStatus.LOCKED ){
            loginAuditService.record(
                    user.getId(),
                    request.email(),
                    false,
                    LoginEventReason.LOCKED,
                    ipAddress
            );

            throw new BadCredentialsException("Invalid email or password");
        }

        if( user.getStatus() == UserStatus.DISABLED){
            loginAuditService.record(
                    user.getId(),
                    request.email(),
                    false,
                    LoginEventReason.DISABLED,
                    ipAddress
            );

            throw new BadCredentialsException("Invalid email or password");
        }

        Credential credential = credentialRepository.findByUserId(user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Credential not found"));

        if(!passwordPolicyService.matches(request.password(), credential.getPasswordHash())){
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);

            if(user.getFailedLoginAttempts() >= 5){
                user.setStatus(UserStatus.LOCKED);
            }

            userRepository.save(user);

            loginAuditService.record(
                    user.getId(),
                    request.email(),
                    false,
                    LoginEventReason.BAD_PASSWORD,
                    ipAddress
            );

            throw new BadCredentialsException("Invalid email or password");
        }

        user.setFailedLoginAttempts(0);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        EffectiveAccess access = adminClient.getEffectiveAccess(user.getId());

        String accessToken = tokenService.mintAccessToken(
                user.getId(),
                user.getUsername(),
                user.getUsername(),
                access.roles(),
                access.perms(),
                credential.isMustChangePassword(),
                "ro");
        RawAndHashedToken refreshToken = tokenService.generateRefreshToken();

        refreshTokenRepository.save(
                new RefreshToken(
                        refreshToken.hashed(),           // tokenHash — PK
                        user.getId(),                    // userId
                        UUID.randomUUID(),               // familyId — new family for new login
                        Instant.now(),                   // issuedAt
                        Instant.now().plusSeconds(2592000L), // expiresAt — 30 days
                        null,                            // revokedAt — not revoked yet
                        null,                            // replacedByTokenHash — nothing yet
                        deviceInfo                       // from User-Agent header
                )
        );

        loginAuditService.record(
                user.getId(),
                request.email(),
                true,
                null,
                ipAddress
        );

        return new TokenResponse(accessToken, refreshToken.raw());
    }

    public TokenResponse refresh(RefreshRequest request) {

        String hashed = tokenService.hash(request.refreshToken());

        RefreshToken oldToken = refreshTokenRepository.findByTokenHash(hashed)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        if (oldToken.getExpiresAt().isBefore(Instant.now())) {
            throw new BadCredentialsException("Refresh token expired");
        }

        // 3. check not revoked
        if (oldToken.getRevokedAt() != null) {

            List<RefreshToken> family = refreshTokenRepository.findByFamilyId(oldToken.getFamilyId());
            family.forEach(t -> {
                t.setRevokedAt(Instant.now());
                refreshTokenRepository.save(t);
            });
            throw new BadCredentialsException("Refresh token reuse detected - please login again");
        }

        oldToken.setRevokedAt(Instant.now());

        EffectiveAccess access = adminClient.getEffectiveAccess(oldToken.getUserId());
        User user = userRepository.findById(oldToken.getUserId()).orElseThrow();
        Credential credential = credentialRepository.findByUserId(oldToken.getUserId()).orElseThrow();

        String newAccessToken = tokenService.mintAccessToken(
                oldToken.getUserId(),
                user.getUsername(),
                user.getUsername(),
                access.roles(),
                access.perms(),
                credential.isMustChangePassword(),
                "ro");
        RawAndHashedToken newRefreshToken = tokenService.generateRefreshToken();
        oldToken.setReplacedByTokenHash(newRefreshToken.hashed());
        refreshTokenRepository.save(oldToken);

        refreshTokenRepository.save(new RefreshToken(
                newRefreshToken.hashed(),                    // tokenHash
                oldToken.getUserId(),                        // same user
                oldToken.getFamilyId(),                      // same family — key for reuse detection
                Instant.now(),                               // issuedAt
                Instant.now().plusSeconds(2592000L),         // expiresAt — 30 days
                null,                                        // revokedAt
                null,                                        // replacedByTokenHash
                oldToken.getDeviceInfo()                     // same device
        ));

        // 8. return both tokens
        return new TokenResponse(newAccessToken, newRefreshToken.raw());
    }

    // LOGOUT
    // 1. find token by hash
    // 2. revoke entire family (not just the one token)
    public void logout(LogoutRequest request){
        String hashed = tokenService.hash(request.refreshToken());
        RefreshToken oldToken = refreshTokenRepository.findByTokenHash(hashed)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        List<RefreshToken> family = refreshTokenRepository.findByFamilyId(oldToken.getFamilyId());
        family.forEach(t -> t.setRevokedAt(Instant.now()));
        refreshTokenRepository.saveAll(family);

    }

    // LOGOUT ALL
    // 1. find all non-revoked tokens for userId
    // 2. revoke all of them
    public void logoutAll(UUID userId){
       List<RefreshToken> family = refreshTokenRepository.findByUserIdAndRevokedAtIsNull(userId);

        family.forEach(t -> t.setRevokedAt(Instant.now()));
        refreshTokenRepository.saveAll(family);
    }

    public void changePassword(UUID userId, ChangePasswordRequest request) {

        // 1. verify current password
        Credential credential = credentialRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Credential not found"));

        if (!passwordPolicyService.matches(request.currentPassword(), credential.getPasswordHash())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        // 2. validate new password against policy
        passwordPolicyService.validate(request.newPassword());

        // 3. hash new password, update Credential
        credential.setPasswordHash(passwordPolicyService.hash(request.newPassword()));
        credential.setPasswordUpdatedAt(Instant.now());

        // 4. set mustChangePassword = false
        credential.setMustChangePassword(false);
        credentialRepository.save(credential);

        // 5. revoke all refresh tokens — new password = new sessions
        List<RefreshToken> activeTokens = refreshTokenRepository
                .findByUserIdAndRevokedAtIsNull(userId);

        activeTokens.forEach(token -> token.setRevokedAt(Instant.now()));
        refreshTokenRepository.saveAll(activeTokens);
    }

}