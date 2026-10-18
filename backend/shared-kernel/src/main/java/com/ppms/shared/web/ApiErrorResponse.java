package com.ppms.shared.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponse {

    private final Instant timestamp;
    private final int status;
    private final String error;
    private final String code;
    private final String message;
    private final String path;
    private final String traceId;
    private final List<FieldValidationError> fieldErrors;
    private final Map<String, Object> details;

    public static ApiErrorResponse of(int status, String error, String code, String message, String path, String traceId) {
        return ApiErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status)
                .error(error)
                .code(code)
                .message(message)
                .path(path)
                .traceId(traceId)
                .fieldErrors(Collections.emptyList())
                .details(Collections.emptyMap())
                .build();
    }
}
