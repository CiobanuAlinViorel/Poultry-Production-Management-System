package com.example.shared.security;

import com.example.shared.domain.User;
import com.example.shared.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.security.auth.login.AccountLockedException;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int PASSWORD_EXPIRY_DAYS = 90;

    @Transactional
    public AuthenticationResult login(String username, String password) throws AccountLockedException {
        // Find user with employee data
        User user = userRepository.findByUsernameWithEmployee(username)
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        // Check account status
        if (user.getAccountLocked()) {
            throw new AccountLockedException("Account is locked due to multiple failed login attempts");
        }

        if (user.isAccountExpired()) {
            throw new AccountExpiredException("Account has expired");
        }

        if (!user.getIsActive()) {
            throw new AccountDisabledException("Account is disabled");
        }

        // Verify password
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            user.recordFailedLogin(MAX_LOGIN_ATTEMPTS);
            userRepository.save(user);
            throw new BadCredentialsException("Invalid credentials");
        }

        // Check if password expired
        if (user.isPasswordExpired(PASSWORD_EXPIRY_DAYS)) {
            user.forcePasswordChange();
            userRepository.save(user);
            return AuthenticationResult.passwordChangeRequired(user);
        }

        // Check if must change password
        if (user.getMustChangePassword()) {
            return AuthenticationResult.passwordChangeRequired(user);
        }

        // Successful login
        user.recordSuccessfulLogin();
        userRepository.save(user);

        // Generate JWT token
        String token = jwtTokenProvider.generateToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        return AuthenticationResult.success(user, token, refreshToken);
    }

    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        // Verify old password
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        // Validate new password
        validatePasswordStrength(newPassword);

        // Check if new password same as old
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new InvalidPasswordException("New password must be different from current password");
        }

        // Encode and save
        String encodedPassword = passwordEncoder.encode(newPassword);
        user.resetPassword(encodedPassword);

        userRepository.save(user);
    }

    @Transactional
    public void unlockAccount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        user.unlockAccount();
        userRepository.save(user);
    }

    private void validatePasswordStrength(String password) {
        if (password == null || password.length() < 8) {
            throw new InvalidPasswordException("Password must be at least 8 characters long");
        }

        if (!password.matches(".*[A-Z].*")) {
            throw new InvalidPasswordException("Password must contain at least one uppercase letter");
        }

        if (!password.matches(".*[a-z].*")) {
            throw new InvalidPasswordException("Password must contain at least one lowercase letter");
        }

        if (!password.matches(".*\\d.*")) {
            throw new InvalidPasswordException("Password must contain at least one digit");
        }

        if (!password.matches(".*[@#$%^&+=!].*")) {
            throw new InvalidPasswordException("Password must contain at least one special character (@#$%^&+=!)");
        }
    }
}