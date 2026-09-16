package com.example.shared.domain.exception;

public class AccountExpiredException extends RuntimeException {
    public AccountExpiredException(String message) {
        super(message);
    }

    public AccountExpiredException(String message, Throwable cause) {
        super(message, cause);
    }
}