package com.ppms.shared.security;

import com.ppms.shared.domain.ScopeType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JwtTokenValidator {

    private final JwtKeyProvider keyProvider;

    public UserPrincipal parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(keyProvider.getPublicKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return toPrincipal(claims);
        } catch (JwtException e) {
            throw new com.ppms.shared.exception.ValidationException("Invalid or expired token: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private UserPrincipal toPrincipal(Claims claims) {
        UUID userId = UUID.fromString(claims.getSubject());
        String username = claims.get("username", String.class);
        String displayName = claims.get("name", String.class);
        boolean mustChangePassword = Boolean.TRUE.equals(claims.get("mcp", Boolean.class));
        String locale = claims.get("locale", String.class);

        List<Map<String, Object>> roles = claims.get("roles", List.class);
        List<RoleClaim> roleClaims = roles != null ? roles.stream()
                .map(r -> new RoleClaim(
                        (String) r.get("role"),
                        ScopeType.valueOf((String) r.get("scopeType")),
                        r.get("scopeId") != null ? UUID.fromString((String) r.get("scopeId")) : null))
                .toList() : List.of();

        List<String> perms = claims.get("perms", List.class);
        Set<String> permSet = perms != null ? Set.copyOf(perms) : Set.of();

        return new UserPrincipal(userId, username, displayName, roleClaims, permSet, mustChangePassword, locale);
    }
}
