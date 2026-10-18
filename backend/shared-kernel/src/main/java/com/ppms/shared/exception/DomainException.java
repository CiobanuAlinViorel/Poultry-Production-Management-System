package com.ppms.shared.exception;

import lombok.Getter;

import java.util.Collections;
import java.util.Map;

@Getter
public class DomainException extends RuntimeException {

    private final String code;
    private final Map<String, Object> details;

    public DomainException(String code, String message) {
        super(message);
        this.code = code;
        this.details = Collections.emptyMap();
    }

    public DomainException(String code, String message, Map<String, Object> details) {
        super(message);
        this.code = code;
        this.details = details != null ? details : Collections.emptyMap();
    }
}
