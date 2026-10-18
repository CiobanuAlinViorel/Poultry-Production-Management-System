package com.ppms.shared.security;

import com.ppms.shared.domain.ScopeType;
import com.ppms.shared.exception.ForbiddenScopeException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScopeGuardTest {

    private final ScopeGuard guard = new ScopeGuard();
    private final UUID farmId = UUID.randomUUID();

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void systemAdminPassesEveryCheck() {
        authenticate(new RoleClaim(Roles.SYSTEM_ADMIN, ScopeType.GLOBAL, null));

        assertThat(guard.isSystemAdmin()).isTrue();
        assertThat(guard.canRead(Roles.FARM_MANAGER)).isTrue();
        assertThat(guard.canWrite(Roles.FARM_MANAGER)).isTrue();
        assertThat(guard.canWrite()).isTrue();
        assertThat(guard.has(Roles.HATCHERY_SUPERVISOR, ScopeType.HATCHERY, UUID.randomUUID())).isTrue();
        assertThat(guard.anyRoleIn(ScopeType.FARM, farmId, Roles.FARM_MANAGER)).isTrue();
        assertThat(guard.hasPerm("farm:lot:write", ScopeType.FARM, farmId)).isTrue();
        assertThat(guard.canManageScope(ScopeType.GLOBAL, null)).isTrue();
        assertThat(guard.canManageScope(ScopeType.FARM, farmId)).isTrue();
    }

    @Test
    void managerIsReadOnly() {
        authenticate(new RoleClaim(Roles.MANAGER, ScopeType.GLOBAL, null));

        assertThat(guard.canRead(Roles.FARM_MANAGER)).isTrue();
        assertThat(guard.canRead()).isTrue();
        assertThat(guard.canWrite(Roles.FARM_MANAGER)).isFalse();
        assertThat(guard.canWrite()).isFalse();
        assertThat(guard.canManageScope(ScopeType.FARM, farmId)).isFalse();
    }

    @Test
    void moduleRoleIsConfinedToItsModuleAndScope() {
        authenticate(new RoleClaim(Roles.FARM_MANAGER, ScopeType.FARM, farmId));

        assertThat(guard.canRead(Roles.FARM_MANAGER)).isTrue();
        assertThat(guard.canWrite(Roles.FARM_MANAGER)).isTrue();
        assertThat(guard.canRead(Roles.HATCHERY_SUPERVISOR)).isFalse();
        assertThat(guard.canWrite()).isFalse();
        assertThat(guard.has(Roles.FARM_MANAGER, ScopeType.FARM, farmId)).isTrue();
        assertThat(guard.has(Roles.FARM_MANAGER, ScopeType.FARM, UUID.randomUUID())).isFalse();
        assertThat(guard.canManageScope(ScopeType.FARM, farmId)).isTrue();
        assertThat(guard.canManageScope(ScopeType.FARM, UUID.randomUUID())).isFalse();
        assertThat(guard.canManageScope(ScopeType.GLOBAL, null)).isFalse();
        assertThatThrownBy(() -> guard.require(Roles.HATCHERY_SUPERVISOR, ScopeType.HATCHERY, null))
                .isInstanceOf(ForbiddenScopeException.class);
    }

    @Test
    void unauthenticatedCallerIsRejected() {
        assertThatThrownBy(() -> guard.canRead(Roles.FARM_MANAGER)).isInstanceOf(ForbiddenScopeException.class);
    }

    private void authenticate(RoleClaim... roles) {
        UserPrincipal principal = new UserPrincipal(UUID.randomUUID(), "user", "User", List.of(roles), Set.of(), false, "ro");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
}
