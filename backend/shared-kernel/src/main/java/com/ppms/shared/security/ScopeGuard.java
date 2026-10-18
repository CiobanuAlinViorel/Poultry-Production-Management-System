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

    public boolean has(String role, ScopeType scopeType, UUID scopeId) {
        return getPrincipal().getRoles().stream()
                .anyMatch(r -> r.role().equals(role)
                        && r.scopeType().equals(scopeType)
                        && (scopeId == null || scopeId.equals(r.scopeId())));
    }

    public boolean anyRoleIn(ScopeType scopeType, UUID scopeId, String... roles) {
        Set<String> set = Set.of(roles);
        return getPrincipal().getRoles().stream()
                .anyMatch(r -> set.contains(r.role())
                        && r.scopeType().equals(scopeType)
                        && (scopeId == null || scopeId.equals(r.scopeId())));
    }

    public boolean hasPerm(String perm, ScopeType scopeType, UUID scopeId) {
        UserPrincipal p = getPrincipal();
        if (p.getPerms().contains(perm) && matchesScope(p, scopeType, scopeId)) {
            return true;
        }
        return p.getRoles().stream()
                .anyMatch(r -> r.role().equals("SYSTEM_ADMIN") || r.role().equals("MANAGER"));
    }

    public void require(String role, ScopeType scopeType, UUID scopeId) {
        if (!has(role, scopeType, scopeId)) {
            throw new ForbiddenScopeException("Required role " + role + " for scope " + scopeType + "/" + scopeId);
        }
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
