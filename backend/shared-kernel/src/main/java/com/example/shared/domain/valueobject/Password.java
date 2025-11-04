package com.example.shared.domain.valueobject;

import com.example.shared.domain.exception.InvalidPasswordException;
import lombok.Value;
import org.springframework.security.crypto.password.PasswordEncoder;

@Value
public class Password {
    String value;

    public Password(String value) {
        if (value == null || value.length() < 8) {
            throw new InvalidPasswordException("Password must be at least 8 characters long");
        }
        if (!value.matches(".*[A-Z].*")) {
            throw new InvalidPasswordException("Password must contain at least one uppercase letter");
        }
        if (!value.matches(".*[a-z].*")) {
            throw new InvalidPasswordException("Password must contain at least one lowercase letter");
        }
        if (!value.matches(".*\\d.*")) {
            throw new InvalidPasswordException("Password must contain at least one digit");
        }
        if (!value.matches(".*[@#$%^&+=!].*")) {
            throw new InvalidPasswordException("Password must contain at least one special character (@#$%^&+=!)");
        }
        this.value = value;
    }

    public String getHash(PasswordEncoder encoder) {
        return encoder.encode(value);
    }

    public boolean matches(String hashedPassword, PasswordEncoder encoder) {
        return encoder.matches(value, hashedPassword);
    }

    public boolean meetsStrengthRequirements() {
        return value != null &&
                value.length() >= 8 &&
                value.matches(".*[A-Z].*") &&
                value.matches(".*[a-z].*") &&
                value.matches(".*\\d.*") &&
                value.matches(".*[@#$%^&+=!].*");
    }
}