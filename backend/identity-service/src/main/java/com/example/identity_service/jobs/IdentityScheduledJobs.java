package com.example.identity_service.jobs;

import com.example.identity_service.domain.UserStatus;
import com.example.identity_service.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class IdentityScheduledJobs {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final ActivationTokenRepository activationTokenRepository;

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void autoUnlock() {
        Instant cutoff = Instant.now().minus(30, ChronoUnit.MINUTES);
        userRepository.findAll().stream()
                .filter(u -> u.getStatus() == UserStatus.LOCKED && u.getLockedAt() != null && u.getLockedAt().isBefore(cutoff))
                .forEach(u -> {
                    u.setStatus(UserStatus.ACTIVE);
                    u.setFailedLoginAttempts(0);
                    u.setLockedAt(null);
                    userRepository.save(u);
                });
    }

    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void purgeExpiredTokens() {
        Instant now = Instant.now();
        refreshTokenRepository.deleteAll(refreshTokenRepository.findByExpiresAtBefore(now));
        passwordResetTokenRepository.deleteByExpiresAtBefore(now);
        activationTokenRepository.deleteByExpiresAtBefore(now);
        log.info("Purged expired tokens");
    }
}
