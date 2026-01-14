package com.example.shared.domain.service;

import com.example.shared.domain.entity.User;
import com.example.shared.domain.exception.InvalidPasswordException;
import com.example.shared.domain.exception.InvalidTokenException;
import com.example.shared.domain.exception.UserNotFoundException;
import com.example.shared.domain.repository.UserRepository;
import com.example.shared.domain.valueobject.Password;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PasswordManagementDomainService {

    private final UserRepository userRepository;

    public void changeUserPassword(User user, Password currentPassword, Password newPassword, PasswordEncoder encoder) {

        user.validateNewPasswordStrength(newPassword);
        user.changePassword(newPassword, encoder);
    }


    public void validatePasswordCompliance(Password password) {
        if (!password.meetsStrengthRequirements()) {
            throw new InvalidPasswordException("Password does not meet strength requirements");
        }
    }

    public boolean isPasswordExpired(User user) {
        return user.isPasswordExpired();
    }
}
