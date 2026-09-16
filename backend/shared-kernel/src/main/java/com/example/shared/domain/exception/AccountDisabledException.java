package com.example.shared.domain.exception;

import org.springframework.security.core.AuthenticationException;

public class AccountDisabledException extends AuthenticationException {
    public AccountDisabledException(String message) {
        super(message);
    }

    public AccountDisabledException(String message, Throwable cause) {
        super(message, cause);
    }
}