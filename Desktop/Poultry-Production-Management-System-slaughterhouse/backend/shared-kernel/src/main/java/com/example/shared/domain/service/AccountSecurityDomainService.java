package com.example.shared.domain.service;

import com.example.shared.domain.entity.User;
//import com.example.shared.domain.exception.AccountSecurityException;
import com.example.shared.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountSecurityDomainService {

    private final UserRepository userRepository;

    public void lockUserAccount(User user, String reason) {
        user.lockAccount(reason);
        userRepository.save(user);
    }

    public void unlockUserAccount(User user) {
        user.unlockAccount();
        userRepository.save(user);
    }

    public void enableUserAccount(User user) {
        user.enableAccount();
        userRepository.save(user);
    }

    public void disableUserAccount(User user, String reason) {
        user.disableAccount(reason);
        userRepository.save(user);
    }

    public void recordFailedLoginAttempt(User user) {
        user.recordFailedLoginAttempt();

        if (user.hasExceededMaxLoginAttempts()) {
            lockUserAccount(user, "Maximum login attempts exceeded");
        }

        userRepository.save(user);
    }

    public void resetFailedLoginAttempts(User user) {
        user.resetFailedLoginAttempts();
        userRepository.save(user);
    }

//    public boolean isAccountCompromised(User user) {
//        return user.isAccountLocked() ||
//                user.hasSuspiciousActivity() ||
//                user.hasExpiredCredentials();
//    }

    public void forcePasswordChange(User user, String reason) {
        //user.forcePasswordChange(reason);
        userRepository.save(user);
    }
}