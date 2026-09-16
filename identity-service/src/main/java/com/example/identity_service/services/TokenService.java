package com.example.identity_service.services;

import com.example.identity_service.dtos.RawAndHashedToken;
import com.ppms.shared.security.JwtKeyProvider;
import com.ppms.shared.security.RoleClaim;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.*;

@Service
public class TokenService {

    private final JwtKeyProvider jwtKeyProvider;  // from shared-kernel — holds RSA key pair

    public TokenService(JwtKeyProvider jwtKeyProvider) {
        this.jwtKeyProvider = jwtKeyProvider;
    }
    // 1. MINT ACCESS TOKEN
    // builds JWT with all claims, signs with RSA private key
    public String mintAccessToken(UUID userId, List<RoleClaim> roles) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(900); // 15 min

        return Jwts.builder()
                .subject(userId.toString())           // sub claim
                .id(UUID.randomUUID().toString())     // jti — unique token ID
                .issuedAt(Date.from(now))             // iat
                .expiration(Date.from(expiry))        // exp
                .claim("roles", roles)                // custom roles claim
                .signWith(jwtKeyProvider.getPrivateKey()) // RS256
                .compact();
    }

    // 2. GENERATE REFRESH TOKEN
    // opaque random value — nothing encoded inside it
    // returns both raw (send to client) and hash (store in DB)
    public RawAndHashedToken generateRefreshToken() {
        String raw = UUID.randomUUID().toString() +
                UUID.randomUUID().toString(); // extra entropy
        String hashed = hash(raw);
        return new RawAndHashedToken(raw, hashed);
    }

    // 3. HASH — SHA-256, used for refresh tokens
    public String hash(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    // 4. VALIDATE ACCESS TOKEN
    // verifies signature + expiry using RSA public key
    // returns claims if valid, throws if not
    public Claims validateAccessToken(String token) {
        return Jwts.parser()
                .verifyWith(jwtKeyProvider.getPublicKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // 5. GET JWK SET
    // returns RSA public key as JWK — served at /.well-known/jwks.json
    // other services use this to validate tokens without calling identity-service
    public Map<String, Object> getJwkSet() {
        RSAPublicKey publicKey = jwtKeyProvider.getPublicKey();
        return Map.of(
                "keys", List.of(Map.of(
                        "kty", "RSA",
                        "use", "sig",
                        "alg", "RS256",
                        "n", Base64.getUrlEncoder().encodeToString(
                                publicKey.getModulus().toByteArray()),
                        "e", Base64.getUrlEncoder().encodeToString(
                                publicKey.getPublicExponent().toByteArray())
                ))
        );
    }
}