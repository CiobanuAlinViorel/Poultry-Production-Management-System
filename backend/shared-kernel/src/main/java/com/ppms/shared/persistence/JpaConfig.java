package com.ppms.shared.persistence;

import com.ppms.shared.security.UserPrincipal;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "currentUserAuditor")
public class JpaConfig {

    @Bean
    public AuditorAware<UUID> currentUserAuditor() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()
                    || authentication.getPrincipal() instanceof String) {
                return Optional.empty();
            }
            if (authentication.getPrincipal() instanceof UserPrincipal principal) {
                return Optional.of(principal.getUserId());
            }
            return Optional.empty();
        };
    }
}
