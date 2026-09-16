package com.example.identity_service.services;

import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PasswordPolicyService {

    private final BCryptPasswordEncoder passwordEncoder;

    // called when creating a user or changing password
    public void validate(String password) {
        if (password == null || password.isBlank()) {
            throw new ValidationException("Password cannot be blank");
        }
        if (password.length() < 8) {
            throw new ValidationException("Password must be at least 8 characters");
        }
        if (password.chars().noneMatch(Character::isUpperCase)) {
            throw new ValidationException("Password must contain at least one uppercase letter");
        }
        if (password.chars().noneMatch(Character::isDigit)) {
            throw new ValidationException("Password must contain at least one digit");
        }
        if (password.chars().noneMatch(c -> "!@#$%^&*()_+-=[]{}|;':\",./<>?".indexOf(c) >= 0)) {
            throw new ValidationException("Password must contain at least one special character");
        }
    }

    // hash with bcrypt cost 12
    public String hash(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    // verify raw password against stored hash
    public boolean matches(String rawPassword, String storedHash) {
        return passwordEncoder.matches(rawPassword, storedHash);
    }
}
