package com.ppms.gateway;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_PATHS = {
            "/api/identity/auth/login",
            "/api/identity/auth/refresh",
            "/api/identity/auth/forgot-password",
            "/api/identity/auth/reset-password",
            "/api/identity/auth/activate",
            "/api/identity/auth/mfa/verify",
            "/api/identity/auth/pin-login",
            "/api/identity/.well-known/**",
            "/actuator/health",
            "/actuator/info"
    };

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(PUBLIC_PATHS).permitAll()
                        .pathMatchers("/api/**").authenticated()
                        .pathMatchers("/internal/**", "/actuator/prometheus").denyAll()
                        .anyExchange().permitAll()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwkSetUri("http://localhost:8084/.well-known/jwks.json"))
                        .authenticationEntryPoint((exchange, ex) -> Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized", ex)))
                )
                .build();
    }
}
