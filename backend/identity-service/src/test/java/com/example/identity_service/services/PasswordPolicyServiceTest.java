package com.example.identity_service.services;

import jakarta.validation.ValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordPolicyServiceTest {

    private final PasswordPolicyService service = new PasswordPolicyService(new BCryptPasswordEncoder());

    @Test
    void acceptsValidPassword() {
        service.validate("StrongPass1!");
    }

    @Test
    void rejectsTooShort() {
        assertThatThrownBy(() -> service.validate("Short1!"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("at least 10");
    }

    @Test
    void rejectsMissingUppercase() {
        assertThatThrownBy(() -> service.validate("lowercase1!"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("uppercase");
    }

    @Test
    void rejectsMissingDigit() {
        assertThatThrownBy(() -> service.validate("NoDigitHere!"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("digit");
    }

    @Test
    void rejectsMissingSpecial() {
        assertThatThrownBy(() -> service.validate("NoSpecial123"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("special character");
    }

    @Test
    void rejectsCommonPassword() {
        assertThatThrownBy(() -> service.validate("MyPassword1!"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("too common");
    }

    @Test
    void rejectsPasswordEqualToUsername() {
        assertThatThrownBy(() -> service.validate("John.Doe1!", "John.Doe1!", null, null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("same as username");
    }

    @Test
    void rejectsPreviouslyUsedPassword() {
        String oldHash = service.hash("OldPass123!");
        assertThatThrownBy(() -> service.validate("OldPass123!", "user", "user@example.com", List.of(oldHash)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("used recently");
    }

    @Test
    void hashAndMatchWork() {
        String hash = service.hash("ValidPass1!");
        assertThat(service.matches("ValidPass1!", hash)).isTrue();
        assertThat(service.matches("WrongPass1!", hash)).isFalse();
    }
}
