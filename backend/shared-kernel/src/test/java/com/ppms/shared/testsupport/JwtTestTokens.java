package com.ppms.shared.testsupport;

import com.ppms.shared.domain.ScopeType;
import com.ppms.shared.security.RoleClaim;
import io.jsonwebtoken.Jwts;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class JwtTestTokens {

    private static final KeyPair KEY_PAIR;

    static {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            KEY_PAIR = gen.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private JwtTestTokens() {
    }

    public static String mint(UUID userId, String role, ScopeType scopeType, UUID scopeId) {
        return Jwts.builder()
                .subject(userId.toString())
                .claim("username", "testuser")
                .claim("name", "Test User")
                .claim("locale", "ro")
                .claim("mcp", false)
                .claim("roles", List.of(new RoleClaim(role, scopeType, scopeId)))
                .claim("perms", Set.of())
                .issuer("ppms-identity")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plus(15, ChronoUnit.MINUTES)))
                .signWith(KEY_PAIR.getPrivate())
                .compact();
    }

    public static RSAPublicKey publicKey() {
        return (RSAPublicKey) KEY_PAIR.getPublic();
    }

    public static RSAPrivateKey privateKey() {
        return (RSAPrivateKey) KEY_PAIR.getPrivate();
    }
}
