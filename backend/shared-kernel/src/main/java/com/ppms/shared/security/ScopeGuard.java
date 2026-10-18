package com.ppms.shared.security;

import com.ppms.shared.domain.ScopeType;
import com.ppms.shared.exception.ForbiddenScopeException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component("scope")
public class ScopeGuard {

    public static final String SYSTEM_ADMIN = Roles.SYSTEM_ADMIN;

    public boolean isSystemAdmin() {
        return isSystemAdmin(getPrincipal());
    }

    /**
     * Module-level read gate: SYSTEM_ADMIN, the read-only MANAGER role, or any of the
     * given module roles (in any scope). Scope-specific checks belong in the services.
     */
    public boolean canRead(String... roles) {
        UserPrincipal p = getPrincipal();
        return isSystemAdmin(p) || hasAnyRole(p, Roles.MANAGER) || hasAnyRole(p, roles);
    }

    /** Module-level write gate: SYSTEM_ADMIN or any of the given module roles (in any scope). */
    public boolean canWrite(String... roles) {
        UserPrincipal p = getPrincipal();
        return isSystemAdmin(p) || hasAnyRole(p, roles);
    }

    /**
     * Whether the caller may manage configuration for the given scope: SYSTEM_ADMIN for
     * anything, otherwise a role held in exactly that scope (e.g. FARM_MANAGER for FARM/{id}).
     * GLOBAL scope is reserved for SYSTEM_ADMIN.
     */
    public boolean canManageScope(ScopeType scopeType, UUID scopeId) {
        UserPrincipal p = getPrincipal();
        if (isSystemAdmin(p)) {
            return true;
        }
        if (scopeType == null || scopeType == ScopeType.GLOBAL) {
            return false;
        }
        return p.getRoles().stream().anyMatch(r -> !Roles.MANAGER.equals(r.role())
                && scopeType.equals(r.scopeType())
                && (r.scopeId() == null || r.scopeId().equals(scopeId)));
    }

    private static boolean hasAnyRole(UserPrincipal p, String... roles) {
        Set<String> set = Set.of(roles);
        return p.getRoles().stream().anyMatch(r -> set.contains(r.role()));
    }

    public boolean has(String role, ScopeType scopeType, UUID scopeId) {
        UserPrincipal p = getPrincipal();
        return isSystemAdmin(p) || p.getRoles().stream()
                .anyMatch(r -> r.role().equals(role)
                        && r.scopeType().equals(scopeType)
                        && (scopeId == null || scopeId.equals(r.scopeId())));
    }

    public boolean anyRoleIn(ScopeType scopeType, UUID scopeId, String... roles) {
        UserPrincipal p = getPrincipal();
        Set<String> set = Set.of(roles);
        return isSystemAdmin(p) || p.getRoles().stream()
                .anyMatch(r -> set.contains(r.role())
                        && r.scopeType().equals(scopeType)
                        && (scopeId == null || scopeId.equals(r.scopeId())));
    }

    public boolean hasPerm(String perm, ScopeType scopeType, UUID scopeId) {
        UserPrincipal p = getPrincipal();
        if (isSystemAdmin(p)) {
            return true;
        }
        if (p.getPerms().contains(perm) && matchesScope(p, scopeType, scopeId)) {
            return true;
        }
        return p.getRoles().stream().anyMatch(r -> r.role().equals("MANAGER"));
    }

    public void require(String role, ScopeType scopeType, UUID scopeId) {
        if (!has(role, scopeType, scopeId)) {
            throw new ForbiddenScopeException("Required role " + role + " for scope " + scopeType + "/" + scopeId);
        }
    }

    private static boolean isSystemAdmin(UserPrincipal p) {
        return p.getRoles().stream()
                .anyMatch(r -> SYSTEM_ADMIN.equals(r.role()) && ScopeType.GLOBAL.equals(r.scopeType()));
    }

    private boolean matchesScope(UserPrincipal p, ScopeType scopeType, UUID scopeId) {
        return p.getRoles().stream().anyMatch(r ->
                r.scopeType().equals(ScopeType.GLOBAL)
                        || (r.scopeType().equals(scopeType) && (scopeId == null || scopeId.equals(r.scopeId()))));
    }

    private UserPrincipal getPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal p)) {
            throw new ForbiddenScopeException("Not authenticated");
        }
        return p;
    }
}
