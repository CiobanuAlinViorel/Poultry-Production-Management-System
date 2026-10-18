package com.ppms.shared.exception;

import java.util.Map;

public class ValidationException extends DomainException {

    public ValidationException(String message) {
        super("VALIDATION_FAILED", message);
    }

    public ValidationException(String message, Map<String, Object> details) {
        super("VALIDATION_FAILED", message, details);
    }
}
