package com.example.identity_service.domain;

public enum LoginEventReason {
    BAD_PASSWORD,
    LOCKED,
    DISABLED,
    UNKNOWN_EMAIL
}
