package com.ppms.shared.exception;

public class ForbiddenScopeException extends DomainException {

    public ForbiddenScopeException(String message) {
        super("FORBIDDEN_SCOPE", message);
    }
}
