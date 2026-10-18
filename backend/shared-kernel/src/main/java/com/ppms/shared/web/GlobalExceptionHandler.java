package com.ppms.shared.web;

import com.ppms.shared.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(EntityNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getCode(), ex.getMessage(), request, null, ex.getDetails());
    }

    @ExceptionHandler({ValidationException.class, MethodArgumentNotValidException.class})
    public ResponseEntity<ApiErrorResponse> handleValidation(Exception ex, HttpServletRequest request) {
        List<FieldValidationError> errors = List.of();
        String message = ex.getMessage();
        if (ex instanceof MethodArgumentNotValidException m) {
            errors = m.getBindingResult().getFieldErrors().stream()
                    .map(this::toFieldError)
                    .toList();
            message = "Validation failed";
        }
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", message, request, errors, Map.of());
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessRule(BusinessRuleViolationException ex, HttpServletRequest request) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getCode(), ex.getMessage(), request, null, ex.getDetails());
    }

    @ExceptionHandler({ConcurrencyConflictException.class, org.springframework.orm.ObjectOptimisticLockingFailureException.class, jakarta.persistence.OptimisticLockException.class})
    public ResponseEntity<ApiErrorResponse> handleConflict(Exception ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "CONCURRENCY_CONFLICT",
                "Resource was modified concurrently. Please reload and retry.", request, null, Map.of());
    }

    @ExceptionHandler({ForbiddenScopeException.class, AccessDeniedException.class})
    public ResponseEntity<ApiErrorResponse> handleForbidden(Exception ex, HttpServletRequest request) {
        String code = ex instanceof ForbiddenScopeException f ? f.getCode() : "FORBIDDEN_SCOPE";
        return build(HttpStatus.FORBIDDEN, code, ex.getMessage(), request, null, Map.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        String code = "DUPLICATE";
        return build(HttpStatus.CONFLICT, code, "Data conflict: " + ex.getMostSpecificCause().getMessage(), request, null, Map.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected error occurred", request, null, Map.of());
    }

    private FieldValidationError toFieldError(FieldError error) {
        return new FieldValidationError(error.getField(), error.getRejectedValue(),
                Optional.ofNullable(error.getDefaultMessage()).orElse("Invalid value"));
    }

    private ResponseEntity<ApiErrorResponse> build(HttpStatus status, String code, String message,
                                                   HttpServletRequest request, List<FieldValidationError> fieldErrors,
                                                   Map<String, Object> details) {
        ApiErrorResponse response = ApiErrorResponse.builder()
                .timestamp(java.time.Instant.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .code(code)
                .message(message)
                .path(request.getRequestURI())
                .traceId(CorrelationId.get())
                .fieldErrors(fieldErrors)
                .details(details)
                .build();
        return ResponseEntity.status(status).body(response);
    }
}
