package com.ppms.shared.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class UserPrincipal implements UserDetails {

    private final UUID userId;
    private final String username;
    private final String displayName;
    private final List<RoleClaim> roles;
    private final Set<String> perms;
    private final boolean mustChangePassword;
    private final String locale;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(UUID userId, String username, String displayName, List<RoleClaim> roles,
                         Set<String> perms, boolean mustChangePassword, String locale) {
        this.userId = userId;
        this.username = username;
        this.displayName = displayName;
        this.roles = roles != null ? roles : List.of();
        this.perms = perms != null ? perms : Set.of();
        this.mustChangePassword = mustChangePassword;
        this.locale = locale != null ? locale : "ro";
        this.authorities = buildAuthorities(roles, perms);
    }

    private static Collection<? extends GrantedAuthority> buildAuthorities(List<RoleClaim> roles, Set<String> perms) {
        var roleAuths = roles.stream()
                .map(claim -> new SimpleGrantedAuthority("ROLE_" + claim.role()))
                .collect(Collectors.toList());
        var permAuths = perms.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
        roleAuths.addAll(permAuths);
        return roleAuths;
    }

    public UUID getUserId() { return userId; }
    public String getDisplayName() { return displayName; }
    public List<RoleClaim> getRoles() { return roles; }
    public Set<String> getPerms() { return perms; }
    public boolean isMustChangePassword() { return mustChangePassword; }
    public String getLocale() { return locale; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }

    @Override
    public String getPassword() { return null; }

    @Override
    public String getUsername() { return username; }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}
