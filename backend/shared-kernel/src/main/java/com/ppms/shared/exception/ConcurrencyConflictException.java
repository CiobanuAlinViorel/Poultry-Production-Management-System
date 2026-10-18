package com.ppms.shared.exception;

public class ConcurrencyConflictException extends DomainException {

    public ConcurrencyConflictException(String message) {
        super("CONCURRENCY_CONFLICT", message);
    }
}
