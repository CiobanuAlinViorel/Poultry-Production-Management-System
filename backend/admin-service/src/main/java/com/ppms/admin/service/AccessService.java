package com.ppms.admin.service;

import com.ppms.admin.api.access.AccessDtos.*;
import com.ppms.admin.domain.Permission;
import com.ppms.admin.domain.Role;
import com.ppms.admin.domain.RolePermission;
import com.ppms.admin.domain.SiteDirectoryEntry;
import com.ppms.admin.domain.UserRoleAssignment;
import com.ppms.admin.repositories.PermissionRepository;
import com.ppms.admin.repositories.RolePermissionRepository;
import com.ppms.admin.repositories.RoleRepository;
import com.ppms.admin.repositories.SiteDirectoryEntryRepository;
import com.ppms.admin.repositories.UserRoleAssignmentRepository;
import com.ppms.shared.domain.ScopeType;
import com.ppms.shared.exception.BusinessRuleViolationException;
import com.ppms.shared.exception.EntityNotFoundException;
import com.ppms.shared.exception.ValidationException;
import com.ppms.shared.security.Roles;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Roles, permissions and user role assignments (the access model consumed by identity at login). */
@Service
@RequiredArgsConstructor
public class AccessService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserRoleAssignmentRepository assignmentRepository;
    private final SiteDirectoryEntryRepository siteDirectoryRepository;

    // ---------------------------------------------------------------- roles

    @Transactional(readOnly = true)
    public List<RoleDto> listRoles() {
        List<Role> roles = roleRepository.findAll();
        Map<UUID, List<String>> permsByRole = permissionsByRole(roles.stream().map(Role::getId).toList());
        return roles.stream()
                .sorted(Comparator.comparing(Role::getCode))
                .map(r -> toDto(r, permsByRole.getOrDefault(r.getId(), List.of()),
                        assignmentRepository.countByRoleIdAndRevokedAtIsNull(r.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public RoleDto getRole(UUID id) {
        Role role = roleRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Role", id));
        return toDto(role, permissionKeys(role.getId()), assignmentRepository.countByRoleIdAndRevokedAtIsNull(id));
    }

    @Transactional
    public RoleDto createRole(RoleRequest request) {
        String code = normalizeCode(request.code());
        roleRepository.findByCode(code).ifPresent(r -> {
            throw new ValidationException("Role code already exists: " + code);
        });
        Role role = new Role();
        role.setCode(code);
        role.setSystemDefined(false);
        applyRole(role, request);
        Role saved = roleRepository.save(role);
        List<String> perms = replacePermissions(saved, request.permissions());
        return toDto(saved, perms, 0);
    }

    @Transactional
    public RoleDto updateRole(UUID id, RoleRequest request) {
        Role role = roleRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Role", id));
        String code = normalizeCode(request.code());
        if (role.isSystemDefined() && !role.getCode().equals(code)) {
            throw new BusinessRuleViolationException("SYSTEM_ROLE_CODE_LOCKED", "System roles cannot be renamed");
        }
        roleRepository.findByCode(code).filter(other -> !other.getId().equals(id)).ifPresent(r -> {
            throw new ValidationException("Role code already exists: " + code);
        });
        role.setCode(code);
        applyRole(role, request);
        List<String> perms = request.permissions() != null ? replacePermissions(role, request.permissions()) : permissionKeys(id);
        return toDto(roleRepository.save(role), perms, assignmentRepository.countByRoleIdAndRevokedAtIsNull(id));
    }

    @Transactional
    public RoleDto setRolePermissions(UUID id, Set<String> permissionKeys) {
        Role role = roleRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Role", id));
        List<String> perms = replacePermissions(role, permissionKeys);
        return toDto(role, perms, assignmentRepository.countByRoleIdAndRevokedAtIsNull(id));
    }

    private void applyRole(Role role, RoleRequest request) {
        role.setNameRo(request.nameRo());
        role.setNameEn(request.nameEn() != null ? request.nameEn() : request.nameRo());
        role.setDescription(request.description());
        role.setModule(request.module() != null ? request.module().toUpperCase(Locale.ROOT) : "CUSTOM");
        if (request.allowedScopeTypes() != null && !request.allowedScopeTypes().isEmpty()) {
            role.setAllowedScopeTypes(new HashSet<>(request.allowedScopeTypes()));
        } else if (role.getAllowedScopeTypes().isEmpty()) {
            role.setAllowedScopeTypes(new HashSet<>(Set.of(ScopeType.GLOBAL)));
        }
        if (request.active() != null) {
            role.setActive(request.active());
        }
    }

    private List<String> replacePermissions(Role role, Set<String> keys) {
        Set<String> wanted = keys == null ? Set.of() : keys;
        List<Permission> permissions = wanted.isEmpty() ? List.of() : permissionRepository.findByKeyIn(wanted);
        if (permissions.size() != wanted.size()) {
            Set<String> found = permissions.stream().map(Permission::getKey).collect(Collectors.toSet());
            Set<String> missing = new TreeSet<>(wanted);
            missing.removeAll(found);
            throw new ValidationException("Unknown permissions: " + String.join(", ", missing));
        }
        rolePermissionRepository.deleteByRoleId(role.getId());
        rolePermissionRepository.flush();
        for (Permission p : permissions) {
            RolePermission rp = new RolePermission();
            rp.setRoleId(role.getId());
            rp.setPermissionId(p.getId());
            rolePermissionRepository.save(rp);
        }
        return permissions.stream().map(Permission::getKey).sorted().toList();
    }

    private List<String> permissionKeys(UUID roleId) {
        return rolePermissionRepository.findByRoleId(roleId).stream()
                .map(rp -> rp.getPermission().getKey()).sorted().toList();
    }

    private Map<UUID, List<String>> permissionsByRole(Collection<UUID> roleIds) {
        if (roleIds.isEmpty()) return Map.of();
        return rolePermissionRepository.findByRoleIdIn(roleIds).stream()
                .collect(Collectors.groupingBy(RolePermission::getRoleId,
                        Collectors.mapping(rp -> rp.getPermission().getKey(), Collectors.collectingAndThen(
                                Collectors.toList(), l -> l.stream().sorted().toList()))));
    }

    // ---------------------------------------------------------- permissions

    @Transactional(readOnly = true)
    public List<PermissionDto> listPermissions() {
        return permissionRepository.findAll().stream()
                .sorted(Comparator.comparing(Permission::getKey))
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public PermissionDto createPermission(PermissionRequest request) {
        String key = request.key().trim().toLowerCase(Locale.ROOT);
        if (!key.matches("[a-z0-9_-]+(:[a-z0-9_-]+){1,2}")) {
            throw new ValidationException("Permission key must look like module:resource:action, got: " + key);
        }
        permissionRepository.findByKey(key).ifPresent(p -> {
            throw new ValidationException("Permission already exists: " + key);
        });
        Permission permission = new Permission();
        permission.setKey(key);
        permission.setModule(request.module() != null ? request.module().toUpperCase(Locale.ROOT)
                : key.substring(0, key.indexOf(':')).toUpperCase(Locale.ROOT));
        permission.setDescription(request.description());
        permission.setSystemDefined(false);
        return toDto(permissionRepository.save(permission));
    }

    // ---------------------------------------------------------- assignments

    @Transactional(readOnly = true)
    public List<AssignmentDto> listAssignments(UUID userId, boolean includeInactive) {
        return toAssignmentDtos(assignmentRepository.findByUserId(userId), includeInactive);
    }

    @Transactional(readOnly = true)
    public Map<UUID, List<AssignmentDto>> listAssignmentsByUsers(Collection<UUID> userIds) {
        if (userIds.isEmpty()) return Map.of();
        return toAssignmentDtos(assignmentRepository.findByUserIdIn(userIds), false).stream()
                .collect(Collectors.groupingBy(AssignmentDto::userId));
    }

    @Transactional
    public AssignmentDto assign(AssignmentRequest request, UUID assignedBy) {
        Role role = roleRepository.findByCode(normalizeCode(request.roleCode()))
                .orElseThrow(() -> new EntityNotFoundException("Role", request.roleCode()));
        if (!role.isActive()) {
            throw new BusinessRuleViolationException("ROLE_INACTIVE", "Role " + role.getCode() + " is inactive");
        }
        if (!role.getAllowedScopeTypes().contains(request.scopeType())) {
            throw new BusinessRuleViolationException("SCOPE_NOT_ALLOWED",
                    "Role " + role.getCode() + " cannot be assigned at scope " + request.scopeType(),
                    Map.of("allowedScopeTypes", role.getAllowedScopeTypes()));
        }
        UUID scopeId = request.scopeType() == ScopeType.GLOBAL ? null : request.scopeId();
        if (request.scopeType() != ScopeType.GLOBAL && scopeId == null) {
            throw new ValidationException("scopeId is required for scope type " + request.scopeType());
        }
        Instant now = Instant.now();
        boolean duplicate = assignmentRepository.findByUserId(request.userId()).stream()
                .anyMatch(a -> a.isActive(now) && a.getRole().getId().equals(role.getId())
                        && a.getScopeType() == request.scopeType() && Objects.equals(a.getScopeId(), scopeId));
        if (duplicate) {
            throw new BusinessRuleViolationException("ASSIGNMENT_EXISTS", "User already has this role in this scope");
        }
        UserRoleAssignment a = new UserRoleAssignment();
        a.setUserId(request.userId());
        a.setRole(role);
        a.setScopeType(request.scopeType());
        a.setScopeId(scopeId);
        a.setAssignedBy(assignedBy);
        a.setAssignedAt(now);
        a.setValidFrom(request.validFrom() != null ? request.validFrom() : now);
        a.setValidTo(request.validTo());
        a.setReason(request.reason());
        return toAssignmentDtos(List.of(assignmentRepository.save(a)), true).get(0);
    }

    @Transactional
    public void revoke(UUID assignmentId, UUID revokedBy, UUID actingUserId) {
        UserRoleAssignment a = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new EntityNotFoundException("UserRoleAssignment", assignmentId));
        if (a.getRevokedAt() != null) return;
        if (a.getUserId().equals(actingUserId) && Roles.SYSTEM_ADMIN.equals(a.getRole().getCode())) {
            throw new BusinessRuleViolationException("CANNOT_REVOKE_OWN_ADMIN", "You cannot revoke your own SYSTEM_ADMIN role");
        }
        a.setRevokedAt(Instant.now());
        a.setRevokedBy(revokedBy);
    }

    private List<AssignmentDto> toAssignmentDtos(List<UserRoleAssignment> assignments, boolean includeInactive) {
        Instant now = Instant.now();
        List<UserRoleAssignment> visible = assignments.stream()
                .filter(a -> includeInactive || a.isActive(now))
                .sorted(Comparator.comparing(UserRoleAssignment::getAssignedAt).reversed())
                .toList();
        Set<UUID> siteIds = visible.stream().map(UserRoleAssignment::getScopeId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<UUID, SiteDirectoryEntry> sites = siteIds.isEmpty() ? Map.of()
                : siteDirectoryRepository.findBySiteIdIn(siteIds).stream()
                .collect(Collectors.toMap(SiteDirectoryEntry::getSiteId, Function.identity()));
        return visible.stream().map(a -> {
            SiteDirectoryEntry site = a.getScopeId() != null ? sites.get(a.getScopeId()) : null;
            return new AssignmentDto(a.getId(), a.getUserId(), a.getRole().getCode(), a.getRole().getNameRo(),
                    a.getScopeType(), a.getScopeId(), site != null ? site.getName() : null, a.getAssignedBy(),
                    a.getAssignedAt(), a.getValidFrom(), a.getValidTo(), a.getRevokedAt(), a.getReason(), a.isActive(now));
        }).toList();
    }

    // ---------------------------------------------------------------- sites

    @Transactional(readOnly = true)
    public List<SiteDto> listSites(ScopeType scopeType) {
        List<SiteDirectoryEntry> entries = scopeType == null ? siteDirectoryRepository.findAll()
                : siteDirectoryRepository.findBySiteTypeOrderByName(scopeType.name());
        return entries.stream()
                .map(s -> new SiteDto(s.getSiteId(), s.getSiteType(), s.getCode(), s.getName(), s.getCounty(), s.isActive()))
                .toList();
    }

    // -------------------------------------------------------------- helpers

    private static String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9_]", "_");
    }

    private RoleDto toDto(Role r, List<String> permissions, long activeAssignments) {
        // copy: the element collection is lazy and must be initialised inside the transaction
        return new RoleDto(r.getId(), r.getCode(), r.getNameRo(), r.getNameEn(), r.getDescription(), r.getModule(),
                EnumSet.copyOf(r.getAllowedScopeTypes().isEmpty() ? Set.of(ScopeType.GLOBAL) : r.getAllowedScopeTypes()),
                r.isSystemDefined(), r.isActive(), permissions, activeAssignments);
    }

    private PermissionDto toDto(Permission p) {
        return new PermissionDto(p.getId(), p.getKey(), p.getModule(), p.getDescription(), p.isSystemDefined());
    }
}
