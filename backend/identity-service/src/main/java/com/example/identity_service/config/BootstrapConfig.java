package com.example.identity_service.config;

import com.example.identity_service.domain.Credential;
import com.example.identity_service.domain.User;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class BootstrapConfig {

    private final UserRepository userRepository;
    private final CredentialRepository credentialRepository;
    private final PasswordPolicyService passwordPolicyService;

    @Value("${PPMS_BOOTSTRAP_ADMIN_EMAIL:admin@ppms.local}")
    private String bootstrapEmail;

    @Value("${PPMS_BOOTSTRAP_ADMIN_PASSWORD:Admin123!}")
    private String bootstrapPassword;

    @Bean
    @Transactional
    public CommandLineRunner bootstrapAdmin() {
        return args -> {
            userRepository.findByEmail(bootstrapEmail).ifPresentOrElse(
                    u -> log.info("Bootstrap admin already exists: {}", bootstrapEmail),
                    () -> {
                        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000000");
                        User user = new User();
                        user.setId(id);
                        user.setEmail(bootstrapEmail);
                        user.setUsername("admin");
                        user.setDisplayName("Administrator");
                        user.setLocale("ro");
                        user.setStatus(UserStatus.ACTIVE);
                        user.setFailedLoginAttempts(0);
                        user.setCreatedAt(Instant.now());
                        user.setUpdatedAt(Instant.now());
                        userRepository.save(user);

                        Credential credential = new Credential();
                        credential.setUserId(id);
                        credential.setPasswordHash(passwordPolicyService.hash(bootstrapPassword));
                        credential.setPasswordUpdatedAt(Instant.now());
                        credential.setMustChangePassword(false);
                        credential.setPasswordHistory(new ArrayList<>());
                        credential.setMfaRecoveryCodes(new ArrayList<>());
                        credential.setCreatedAt(Instant.now());
                        credential.setUpdatedAt(Instant.now());
                        credentialRepository.save(credential);
                        log.info("Created bootstrap admin: {}", bootstrapEmail);
                    }
            );
        };
    }
}
