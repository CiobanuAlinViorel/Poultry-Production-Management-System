package com.ppms.shared.exception;

import java.util.Map;

public class BusinessRuleViolationException extends DomainException {

    public BusinessRuleViolationException(String code, String message) {
        super(code, message);
    }

    public BusinessRuleViolationException(String code, String message, Map<String, Object> details) {
        super(code, message, details);
    }
}
