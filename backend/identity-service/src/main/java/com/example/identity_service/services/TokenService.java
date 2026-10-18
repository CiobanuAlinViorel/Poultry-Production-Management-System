package com.example.identity_service.services;

import com.example.identity_service.dtos.RawAndHashedToken;
import com.ppms.shared.security.JwtKeyProvider;
import com.ppms.shared.security.RoleClaim;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.*;

@Service
public class TokenService {

    private final JwtKeyProvider jwtKeyProvider;

    public TokenService(JwtKeyProvider jwtKeyProvider) {
        this.jwtKeyProvider = jwtKeyProvider;
    }

    public String mintAccessToken(UUID userId, String username, String displayName, List<RoleClaim> roles,
                                  Set<String> perms, boolean mustChangePassword, String locale) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(900);

        return Jwts.builder()
                .subject(userId.toString())
                .id(UUID.randomUUID().toString())
                .issuer("ppms-identity")
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .claim("username", username)
                .claim("name", displayName)
                .claim("roles", roles)
                .claim("perms", perms)
                .claim("mcp", mustChangePassword)
                .claim("locale", locale)
                .header().add("kid", jwtKeyProvider.getKid()).and()
                .signWith(jwtKeyProvider.getPrivateKey())
                .compact();
    }

    public RawAndHashedToken generateRefreshToken() {
        String raw = UUID.randomUUID().toString() + UUID.randomUUID().toString();
        String hashed = hash(raw);
        return new RawAndHashedToken(raw, hashed);
    }

    public String hash(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    public Claims validateAccessToken(String token) {
        return Jwts.parser()
                .verifyWith(jwtKeyProvider.getPublicKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Map<String, Object> getJwkSet() {
        RSAPublicKey publicKey = jwtKeyProvider.getPublicKey();
        return Map.of(
                "keys", List.of(Map.of(
                        "kty", "RSA",
                        "use", "sig",
                        "alg", "RS256",
                        "kid", jwtKeyProvider.getKid(),
                        "n", Base64.getUrlEncoder().encodeToString(
                                publicKey.getModulus().toByteArray()),
                        "e", Base64.getUrlEncoder().encodeToString(
                                publicKey.getPublicExponent().toByteArray())
                ))
        );
    }

    public String generateMfaToken(UUID userId) {
        return Jwts.builder()
                .subject(userId.toString())
                .claim("type", "mfa")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(jwtKeyProvider.getPrivateKey())
                .compact();
    }

    public UUID parseMfaToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(jwtKeyProvider.getPublicKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        if (!"mfa".equals(claims.get("type"))) {
            throw new BadCredentialsException("Invalid MFA token");
        }
        return UUID.fromString(claims.getSubject());
    }
}
