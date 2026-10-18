package com.ppms.admin.api.access;

import com.ppms.shared.domain.ScopeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class AccessDtos {
    private AccessDtos() {}

    public record RoleDto(UUID id, String code, String nameRo, String nameEn, String description, String module,
                          Set<ScopeType> allowedScopeTypes, boolean systemDefined, boolean active,
                          List<String> permissions, long activeAssignments) {}

    public record RoleRequest(@NotBlank String code, @NotBlank String nameRo, String nameEn, String description,
                              String module, Set<ScopeType> allowedScopeTypes, Set<String> permissions, Boolean active) {}

    public record RolePermissionsRequest(@NotNull Set<String> permissions) {}

    public record PermissionDto(UUID id, String key, String module, String description, boolean systemDefined) {}

    public record PermissionRequest(@NotBlank String key, String module, String description) {}

    public record AssignmentDto(UUID id, UUID userId, String roleCode, String roleName, ScopeType scopeType, UUID scopeId,
                                String scopeName, UUID assignedBy, Instant assignedAt, Instant validFrom, Instant validTo,
                                Instant revokedAt, String reason, boolean active) {}

    public record AssignmentRequest(@NotNull UUID userId, @NotBlank String roleCode, @NotNull ScopeType scopeType,
                                    UUID scopeId, Instant validFrom, Instant validTo, String reason) {}

    public record SiteDto(UUID siteId, String siteType, String code, String name, String county, boolean active) {}
}
