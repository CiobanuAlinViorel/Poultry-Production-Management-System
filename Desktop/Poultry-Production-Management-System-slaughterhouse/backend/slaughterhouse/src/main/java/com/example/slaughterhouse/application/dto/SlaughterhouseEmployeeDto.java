package com.example.slaughterhouse.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for SlaughterhouseEmployee entity
 * Represents employee working in slaughterhouse
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SlaughterhouseEmployeeDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("username")
    private String username;

    @JsonProperty("email")
    private String email;

    @JsonProperty("first_name")
    private String firstName;

    @JsonProperty("last_name")
    private String lastName;

    @JsonProperty("employee_code")
    private String employeeCode;

    @JsonProperty("position")
    private String position;

    @JsonProperty("department")
    private String department;

    @JsonProperty("hire_date")
    private String hireDate;

    @JsonProperty("phone_number")
    private String phoneNumber;

    @JsonProperty("is_active")
    private Boolean isActive;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    @JsonProperty("version")
    private Integer version;
}