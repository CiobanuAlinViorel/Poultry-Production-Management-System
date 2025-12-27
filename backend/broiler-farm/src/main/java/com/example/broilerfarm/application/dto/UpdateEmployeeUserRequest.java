package com.example.broilerfarm.application.dto;

import com.example.shared.domain.enums.Role;

public /**
 * DTO pentru actualizarea angajatului și userului
 */
@lombok.Data
@lombok.Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
class UpdateEmployeeUserRequest {
    private String firstName;
    private String lastName;
    private String phone;
    private String username;
    private Role role;
    private Long farmId;
}