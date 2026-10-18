package com.ppms.admin.api.internal;

import com.ppms.admin.domain.Permission;
import com.ppms.admin.domain.RolePermission;
import com.ppms.admin.domain.UserRoleAssignment;
import com.ppms.admin.repositories.PermissionRepository;
import com.ppms.admin.repositories.RolePermissionRepository;
import com.ppms.admin.repositories.UserRoleAssignmentRepository;
import com.ppms.shared.domain.ScopeType;
import com.ppms.shared.security.RoleClaim;
import com.ppms.shared.security.ScopeGuard;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/internal/users")
public class InternalAccessController {

    private final UserRoleAssignmentRepository assignmentRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionRepository permissionRepository;

    public InternalAccessController(UserRoleAssignmentRepository assignmentRepository,
                                    RolePermissionRepository rolePermissionRepository,
                                    PermissionRepository permissionRepository) {
        this.assignmentRepository = assignmentRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.permissionRepository = permissionRepository;
    }

    @GetMapping("/{userId}/effective-access")
    @Transactional(readOnly = true)
    public EffectiveAccessResponse effectiveAccess(@PathVariable("userId") UUID userId) {
        Instant now = Instant.now();
        List<UserRoleAssignment> active = assignmentRepository.findByUserId(userId).stream()
                .filter(a -> a.isActive(now))
                .toList();

        List<RoleClaim> roles = active.stream()
                .map(a -> new RoleClaim(a.getRole().getCode(), a.getScopeType(), a.getScopeId()))
                .toList();

        boolean systemAdmin = roles.stream()
                .anyMatch(r -> ScopeGuard.SYSTEM_ADMIN.equals(r.role()) && ScopeType.GLOBAL.equals(r.scopeType()));

        Set<String> perms = systemAdmin
                ? permissionRepository.findAll().stream().map(Permission::getKey).collect(Collectors.toSet())
                : active.stream()
                .flatMap(a -> rolePermissionRepository.findByRoleId(a.getRole().getId()).stream())
                .map(RolePermission::getPermission)
                .map(Permission::getKey)
                .collect(Collectors.toSet());

        return new EffectiveAccessResponse(roles, perms);
    }

    public record EffectiveAccessResponse(List<RoleClaim> roles, Set<String> perms) {
    }
}
