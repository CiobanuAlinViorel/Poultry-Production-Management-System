package com.ppms.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.List;

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
                .cors(Customizer.withDefaults())
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(PUBLIC_PATHS).permitAll()
                        .pathMatchers("/api/**").authenticated()
                        .pathMatchers("/internal/**", "/actuator/prometheus").denyAll()
                        .anyExchange().permitAll()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint((exchange, ex) -> Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized", ex)))
                )
                .build();
    }

    /** CORS must be handled inside the security chain so preflight requests are answered before authentication. */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${ppms.cors.allowed-origin-patterns:http://localhost:*,http://127.0.0.1:*}") List<String> allowedOriginPatterns) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(allowedOriginPatterns);
        config.setAllowedMethods(List.of("*"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization", "Content-Disposition"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
