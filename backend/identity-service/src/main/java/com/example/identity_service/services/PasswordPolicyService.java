package com.example.identity_service.services;

import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PasswordPolicyService {

    private static final Set<String> COMMON_PASSWORDS = Set.of(
            "password", "password123", "admin123", "qwerty", "123456", "12345678", "welcome1"
    );

    private final BCryptPasswordEncoder passwordEncoder;

    public void validate(String password, String username, String email, List<String> history) {
        if (password == null || password.isBlank()) {
            throw new ValidationException("Password cannot be blank");
        }
        if (password.length() < 10) {
            throw new ValidationException("Password must be at least 10 characters");
        }
        if (password.chars().noneMatch(Character::isUpperCase)) {
            throw new ValidationException("Password must contain at least one uppercase letter");
        }
        if (password.chars().noneMatch(Character::isLowerCase)) {
            throw new ValidationException("Password must contain at least one lowercase letter");
        }
        if (password.chars().noneMatch(Character::isDigit)) {
            throw new ValidationException("Password must contain at least one digit");
        }
        if (password.chars().noneMatch(c -> "!@#$%^&*()_+-=[]{}|;':\",./<>?".indexOf(c) >= 0)) {
            throw new ValidationException("Password must contain at least one special character");
        }
        String localPart = email != null ? email.split("@")[0] : "";
        if (password.equalsIgnoreCase(username) || password.equalsIgnoreCase(localPart) || password.equalsIgnoreCase(email)) {
            throw new ValidationException("Password cannot be the same as username or email");
        }
        String lower = password.toLowerCase();
        if (COMMON_PASSWORDS.stream().anyMatch(lower::contains)) {
            throw new ValidationException("Password is too common");
        }
        if (history != null && history.stream().anyMatch(h -> passwordEncoder.matches(password, h))) {
            throw new ValidationException("Password was used recently");
        }
    }

    public void validate(String password) {
        validate(password, null, null, null);
    }

    public String hash(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    public boolean matches(String rawPassword, String storedHash) {
        return passwordEncoder.matches(rawPassword, storedHash);
    }
}
