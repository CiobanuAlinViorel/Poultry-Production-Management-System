package com.example.broilerfarm.application.dto;

import java.util.ArrayList;

/**
 * DTO pentru răspunsul de autentificare
 */
@lombok.Data
@lombok.Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class AuthenticationResponse {
    private String token;
    private String email;
    private String username;
    private ArrayList<String> roles;
    private Long employeeId;     // ← ADD
    private Long farmId;
}