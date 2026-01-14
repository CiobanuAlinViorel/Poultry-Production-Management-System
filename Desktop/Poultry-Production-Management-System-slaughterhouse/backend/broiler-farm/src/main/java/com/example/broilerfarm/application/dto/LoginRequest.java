package com.example.broilerfarm.application.dto;

/**
 * DTO pentru cererea de login
 */
@lombok.Data
@lombok.Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class LoginRequest {
    private String email;
    private String password;
}