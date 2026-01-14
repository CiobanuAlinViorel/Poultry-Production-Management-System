package com.example.broilerfarm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

/**
 * CORS Configuration
 *
 * Permite request-uri de la frontend (React) către backend (Spring Boot)
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        // ❌ NU folosi allowedOrigins("*") cu allowCredentials(true)
        // ✅ Folosește allowedOriginPatterns SAU specifică exact originea

        // Opțiunea 1: allowedOriginPatterns (permite orice în development)
        config.setAllowedOriginPatterns(Arrays.asList("*"));

        // Opțiunea 2: Specifică exact originea frontend-ului (recomandat pentru producție)
        // config.setAllowedOrigins(Arrays.asList(
        //     "http://localhost:5173",    // Vite dev server
        //     "http://localhost:3000",    // Create React App
        //     "https://your-production-domain.com"  // Production
        // ));

        // Headers permise
        config.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "Accept",
                "X-Requested-With"
        ));

        // Methods permise
        config.setAllowedMethods(Arrays.asList(
                "GET",
                "POST",
                "PUT",
                "DELETE",
                "OPTIONS"
        ));

        // Permite credentials (cookies, authorization headers)
        config.setAllowCredentials(true);

        // Max age pentru preflight requests
        config.setMaxAge(3600L);

        // Aplică configurația pentru toate rutele /api/**
        source.registerCorsConfiguration("/api/**", config);

        return new CorsFilter(source);
    }
}