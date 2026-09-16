package com.example.shared.domain.exception;

public class AccountSecurityException extends RuntimeException {
    public AccountSecurityException(String message) {
        super(message);
    }

    public AccountSecurityException(String message, Throwable cause) {
        super(message, cause);
    }
}