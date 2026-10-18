package com.example.identity_service.services;

import com.example.identity_service.domain.User;
import com.example.identity_service.domain.UserPin;
import com.example.identity_service.domain.UserStatus;
import com.example.identity_service.repositories.UserPinRepository;
import com.example.identity_service.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Records failed login / PIN attempts in their own transaction so the lockout
 * counters survive the BadCredentialsException rollback of the caller.
 */
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    static final int LOCKOUT_THRESHOLD = 5;
    static final int PIN_LOCKOUT_THRESHOLD = 3;
    static final long AUTO_UNLOCK_MINUTES = 30;

    private final UserRepository userRepository;
    private final UserPinRepository userPinRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailedPassword(UUID userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return;
        }
        user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
        if (user.getFailedLoginAttempts() >= LOCKOUT_THRESHOLD) {
            user.setStatus(UserStatus.LOCKED);
            user.setLockedAt(Instant.now());
        }
        userRepository.save(user);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailedPin(UUID userId) {
        UserPin pin = userPinRepository.findByUserId(userId).orElse(null);
        if (pin == null) {
            return;
        }
        pin.setFailedAttempts(pin.getFailedAttempts() + 1);
        if (pin.getFailedAttempts() >= PIN_LOCKOUT_THRESHOLD) {
            pin.setLockedUntil(Instant.now().plus(AUTO_UNLOCK_MINUTES, ChronoUnit.MINUTES));
        }
        userPinRepository.save(pin);
    }
}
