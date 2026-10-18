package com.ppms.shared.security;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Slf4j
@Getter
@Component
public class JwtKeyProvider {

    private final String publicKeyFile;
    private final String publicKeyValue;
    private final String privateKeyFile;
    private final String privateKeyValue;

    private RSAPublicKey publicKey;
    private RSAPrivateKey privateKey;
    private String kid;

    public JwtKeyProvider(
            @Value("${ppms.jwt.public-key-file:}") String publicKeyFile,
            @Value("${ppms.jwt.public-key:}") String publicKeyValue,
            @Value("${ppms.jwt.private-key-file:}") String privateKeyFile,
            @Value("${ppms.jwt.private-key:}") String privateKeyValue) {
        this.publicKeyFile = publicKeyFile;
        this.publicKeyValue = publicKeyValue;
        this.privateKeyFile = privateKeyFile;
        this.privateKeyValue = privateKeyValue;
    }

    @PostConstruct
    public void init() {
        String publicPem = loadPem(publicKeyFile, publicKeyValue, "JWT_PUBLIC_KEY_FILE", "JWT_PUBLIC_KEY");
        String privatePem = loadPem(privateKeyFile, privateKeyValue, "JWT_PRIVATE_KEY_FILE", "JWT_PRIVATE_KEY");

        if (publicPem != null && !publicPem.isBlank()) {
            this.publicKey = parsePublicKey(publicPem);
            this.kid = deriveKid(publicPem);
        }
        if (privatePem != null && !privatePem.isBlank()) {
            this.privateKey = parsePrivateKey(privatePem);
        }

        if (this.publicKey == null && this.privateKey == null) {
            throw new IllegalStateException("Neither JWT public nor private key configured");
        }
    }

    private String loadPem(String fileProperty, String valueProperty, String envFile, String envValue) {
        String fromEnvFile = System.getenv(envFile);
        if (fromEnvFile != null && !fromEnvFile.isBlank()) {
            try {
                return Files.readString(Path.of(fromEnvFile), StandardCharsets.UTF_8);
            } catch (IOException e) {
                log.warn("Could not read {}: {}", envFile, e.getMessage());
            }
        }
        if (fileProperty != null && !fileProperty.isBlank()) {
            try {
                return Files.readString(Path.of(fileProperty), StandardCharsets.UTF_8);
            } catch (IOException e) {
                log.warn("Could not read public key file {}: {}", fileProperty, e.getMessage());
            }
        }
        String fromEnvValue = System.getenv(envValue);
        if (fromEnvValue != null && !fromEnvValue.isBlank()) {
            return fromEnvValue;
        }
        if (valueProperty != null && !valueProperty.isBlank()) {
            return valueProperty;
        }
        return null;
    }

    private String deriveKid(String pem) {
        try {
            String normalized = pem.replaceAll("\\s+", "");
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(normalized.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash).substring(0, 12);
        } catch (Exception e) {
            throw new RuntimeException("Failed to derive key id", e);
        }
    }

    private RSAPrivateKey parsePrivateKey(String pem) {
        try {
            String cleaned = pem
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s+", "");

            byte[] decoded = Base64.getDecoder().decode(cleaned);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse RSA private key", e);
        }
    }

    private RSAPublicKey parsePublicKey(String pem) {
        try {
            String cleaned = pem
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s+", "");

            byte[] decoded = Base64.getDecoder().decode(cleaned);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return (RSAPublicKey) keyFactory.generatePublic(keySpec);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse RSA public key", e);
        }
    }
}
