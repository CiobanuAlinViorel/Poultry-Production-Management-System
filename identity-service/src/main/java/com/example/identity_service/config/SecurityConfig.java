package com.example.identity_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

// Minimal filter chain so /api/v1/auth/** is reachable without HTTP Basic.
// This is a stateless JWT API — no session, no CSRF token to carry.
// NOTE: authenticated endpoints (/logout-all, /change-password) still need a
// JWT resource-server filter to populate the UserPrincipal; that doesn't
// exist yet, so those two remain unauthenticated/unusable until it's added.
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .anyRequest().authenticated()
                );
        return http.build();
    }
}
