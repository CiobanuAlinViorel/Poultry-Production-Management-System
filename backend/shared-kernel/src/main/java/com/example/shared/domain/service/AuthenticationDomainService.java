package com.example.shared.domain.service;


import com.example.shared.domain.entity.User;
import com.example.shared.domain.exception.*;
import com.example.shared.domain.repository.UserRepository;
import com.example.shared.domain.valueobject.Password;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.security.auth.login.AccountLockedException;

@Service
@RequiredArgsConstructor
public class AuthenticationDomainService {

    private final UserRepository userRepository;

    public User authenticateUser(String username, Password password, PasswordEncoder passwordEncoder) throws AccountLockedException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password"));

        user.attemptAuthentication(password, passwordEncoder);

        return user;
    }

    public void validateUserCanAuthenticate(User user) throws AccountLockedException {
        if (!user.isAccountActive()) {
            throw new AccountDisabledException("Account is disabled");
        }

        if (user.isAccountLocked()) {
            throw new AccountLockedException("Account is locked due to multiple failed attempts");
        }

        if (user.isAccountExpired()) {
            throw new AccountExpiredException("Account has expired");
        }
    }

    public boolean requiresPasswordChange(User user) {
        return user.hasExpiredPassword() || user.isPasswordChangeRequired();
    }
}