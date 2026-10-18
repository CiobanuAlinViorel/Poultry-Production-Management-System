package com.ppms.admin.api.access;

import com.ppms.admin.api.access.AccessDtos.*;
import com.ppms.admin.service.AccessService;
import com.ppms.shared.domain.ScopeType;
import com.ppms.shared.security.CurrentUser;
import com.ppms.shared.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Access model administration: roles, permissions, user role assignments. Reads: admin/manager; writes: admin. */
@PreAuthorize("@scope.canRead()")
@RestController
@RequestMapping("/api/v1/access")
@RequiredArgsConstructor
public class AccessController {

    private final AccessService accessService;

    @GetMapping("/roles")
    public List<RoleDto> roles() { return accessService.listRoles(); }

    @GetMapping("/roles/{id}")
    public RoleDto role(@PathVariable UUID id) { return accessService.getRole(id); }

    @PreAuthorize("@scope.canWrite()")
    @PostMapping("/roles")
    @ResponseStatus(HttpStatus.CREATED)
    public RoleDto createRole(@Valid @RequestBody RoleRequest request) { return accessService.createRole(request); }

    @PreAuthorize("@scope.canWrite()")
    @PutMapping("/roles/{id}")
    public RoleDto updateRole(@PathVariable UUID id, @Valid @RequestBody RoleRequest request) {
        return accessService.updateRole(id, request);
    }

    @PreAuthorize("@scope.canWrite()")
    @PutMapping("/roles/{id}/permissions")
    public RoleDto setRolePermissions(@PathVariable UUID id, @Valid @RequestBody RolePermissionsRequest request) {
        return accessService.setRolePermissions(id, request.permissions());
    }

    @GetMapping("/permissions")
    public List<PermissionDto> permissions() { return accessService.listPermissions(); }

    @PreAuthorize("@scope.canWrite()")
    @PostMapping("/permissions")
    @ResponseStatus(HttpStatus.CREATED)
    public PermissionDto createPermission(@Valid @RequestBody PermissionRequest request) {
        return accessService.createPermission(request);
    }

    @GetMapping("/assignments")
    public List<AssignmentDto> assignments(@RequestParam UUID userId,
                                           @RequestParam(defaultValue = "false") boolean includeInactive) {
        return accessService.listAssignments(userId, includeInactive);
    }

    @GetMapping("/assignments/by-users")
    public Map<UUID, List<AssignmentDto>> assignmentsByUsers(@RequestParam Set<UUID> userIds) {
        return accessService.listAssignmentsByUsers(userIds);
    }

    @PreAuthorize("@scope.canWrite()")
    @PostMapping("/assignments")
    @ResponseStatus(HttpStatus.CREATED)
    public AssignmentDto assign(@Valid @RequestBody AssignmentRequest request, @CurrentUser UserPrincipal user) {
        return accessService.assign(request, user != null ? user.getUserId() : null);
    }

    @PreAuthorize("@scope.canWrite()")
    @DeleteMapping("/assignments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@PathVariable UUID id, @CurrentUser UserPrincipal user) {
        UUID actor = user != null ? user.getUserId() : null;
        accessService.revoke(id, actor, actor);
    }

    @GetMapping("/sites")
    public List<SiteDto> sites(@RequestParam(required = false) ScopeType scopeType) {
        return accessService.listSites(scopeType);
    }
}
