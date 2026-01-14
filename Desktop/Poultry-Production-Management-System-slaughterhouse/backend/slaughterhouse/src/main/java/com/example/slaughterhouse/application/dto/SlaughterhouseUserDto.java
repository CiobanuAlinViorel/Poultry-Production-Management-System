package com.example.slaughterhouse.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * DTO for SlaughterhouseUser entity
 * Represents user in the slaughterhouse system
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SlaughterhouseUserDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("username")
    private String username;

    @JsonProperty("email")
    private String email;

    @JsonProperty("first_name")
    private String firstName;

    @JsonProperty("last_name")
    private String lastName;

    @JsonProperty("full_name")
    private String fullName;

    @JsonProperty("roles")
    private Set<String> roles;

    @JsonProperty("is_active")
    private Boolean isActive;

    @JsonProperty("last_login")
    private LocalDateTime lastLogin;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    @JsonProperty("version")
    private Integer version;
}