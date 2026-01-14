package com.example.broilerfarm.application.dto;

@lombok.Data
@lombok.AllArgsConstructor
public class ValidationResponse {
    private boolean valid;
    private String email;
}
