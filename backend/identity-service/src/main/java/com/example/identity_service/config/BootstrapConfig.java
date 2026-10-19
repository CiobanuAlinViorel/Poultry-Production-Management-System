package com.example.identity_service.config;

import com.example.identity_service.domain.Credential;
import com.example.identity_service.domain.UserStatus;
import com.example.identity_service.repositories.CredentialRepository;
import com.example.identity_service.repositories.UserRepository;
import com.example.identity_service.services.PasswordPolicyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class BootstrapConfig {

    /** Fixed id shared with admin-service seed (R__seed_roles_permissions.sql) which grants SYSTEM_ADMIN to this user. */
    static final UUID BOOTSTRAP_ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    private final UserRepository userRepository;
    private final CredentialRepository credentialRepository;
    private final PasswordPolicyService passwordPolicyService;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    @Value("${PPMS_BOOTSTRAP_ADMIN_EMAIL:admin@ppms.local}")
    private String bootstrapEmail;

    @Value("${PPMS_BOOTSTRAP_ADMIN_PASSWORD:Admin123!}")
    private String bootstrapPassword;

    @Bean
    public CommandLineRunner bootstrapAdmin() {
        return args -> transactionTemplate.executeWithoutResult(tx -> {
            if (userRepository.findByEmail(bootstrapEmail).isPresent() || userRepository.existsById(BOOTSTRAP_ADMIN_ID)) {
                log.info("Bootstrap admin already exists: {}", bootstrapEmail);
                return;
            }
            Instant now = Instant.now();
            // Plain JDBC insert so the fixed id is honoured; User.id is @GeneratedValue and save() would replace it.
            jdbcTemplate.update("""
                    INSERT INTO identity.users (id, email, username, display_name, locale, status, failed_login_attempts, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    BOOTSTRAP_ADMIN_ID, bootstrapEmail, "admin", "Administrator", "ro", UserStatus.ACTIVE.name(), 0,
                    Timestamp.from(now), Timestamp.from(now));

            Credential credential = new Credential();
            credential.setUserId(BOOTSTRAP_ADMIN_ID);
            credential.setPasswordHash(passwordPolicyService.hash(bootstrapPassword));
            credential.setPasswordUpdatedAt(now);
            credential.setMustChangePassword(false);
            credential.setPasswordHistory(new ArrayList<>());
            credential.setMfaRecoveryCodes(new ArrayList<>());
            credential.setCreatedAt(now);
            credential.setUpdatedAt(now);
            credentialRepository.save(credential);
            log.info("Created bootstrap admin: {}", bootstrapEmail);
        });
    }
}
