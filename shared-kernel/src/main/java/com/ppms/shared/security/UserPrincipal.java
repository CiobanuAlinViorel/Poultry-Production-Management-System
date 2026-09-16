package com.ppms.shared.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class UserPrincipal implements UserDetails {

    private final UUID userId;
    private final List<RoleClaim> roles;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(UUID userId, List<RoleClaim> roles) {
        this.userId = userId;
        this.roles = roles;
        // map each RoleClaim to a GrantedAuthority
        // this is what makes @PreAuthorize("hasRole('SYSTEM_ADMIN')") work
        this.authorities = roles.stream()
                .map(claim -> new SimpleGrantedAuthority("ROLE_" + claim.role()))
                .collect(Collectors.toList());
    }

    public UUID getUserId() { return userId; }
    public List<RoleClaim> getRoles() { return roles; }

    // ── UserDetails contract ──────────────────────────────────────────

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return null; // not needed — we authenticate via JWT, not username/password here
    }

    @Override
    public String getUsername() {
        return userId.toString();
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}