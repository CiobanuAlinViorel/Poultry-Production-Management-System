package com.example.broilerfarm.application.dto;

import com.example.shared.domain.enums.Role;

import java.time.LocalDate;

public /**
 * DTO pentru crearea angajatului și userului
 */
@lombok.Data
@lombok.Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
class CreateEmployeeUserRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String username;
    private Role role;
    private LocalDate hireDate;
    private Long farmId;
}