package com.ppms.shared.web;

public record FieldValidationError(String field, Object rejectedValue, String message) {
}
