package com.example.broilerfarm.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security Configuration
 *
 * Responsabilități:
 * - Configurează întregul mecanism de securitate
 * - Definește ce endpoint-uri sunt publice/protejate
 * - Integrează JWT filter-ul în lanțul de securitate
 * - Configurează authentication provider și password encoder
 *
 * Logica din spate:
 * Această configurare este INIMA sistemului de securitate și orchestrează:
 * 1. Cum se autentifică userii (JWT)
 * 2. Ce rute sunt protejate și care sunt publice
 * 3. Cum se hash-uiesc parolele (BCrypt)
 * 4. Cum se validează credențialele (UserDetailsService)
 *
 * FLOW-UL UNEI CERERI HTTP:
 * ┌─────────────────────────────────────────────────────────────┐
 * │ Client → HTTP Request                                       │
 * └────────────────┬────────────────────────────────────────────┘
 *                  ▼
 * ┌─────────────────────────────────────────────────────────────┐
 * │ 1. JwtAuthenticationFilter                                  │
 * │    - Extrage și validează JWT token                         │
 * │    - Setează autentificarea în SecurityContext              │
 * └────────────────┬────────────────────────────────────────────┘
 *                  ▼
 * ┌─────────────────────────────────────────────────────────────┐
 * │ 2. Security Filter Chain                                    │
 * │    - Verifică dacă ruta e permisă                           │
 * │    - /api/auth/** → permis fără autentificare               │
 * │    - /api/** → necesită autentificare                       │
 * └────────────────┬────────────────────────────────────────────┘
 *                  ▼
 * ┌─────────────────────────────────────────────────────────────┐
 * │ 3. Controller                                                │
 * │    - Dacă ajunge aici, userul e autentificat                │
 * │    - Poate accesa detalii user cu @AuthenticationPrincipal  │
 * └─────────────────────────────────────────────────────────────┘
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // Permite @PreAuthorize, @Secured pe metode
@RequiredArgsConstructor
public class SecurityConfiguration {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    /**
     * Configurează lanțul de filtre de securitate
     *
     * Aici definim:
     * - Ce rute sunt publice (login, register)
     * - Ce rute necesită autentificare
     * - Ordinea filtrelor
     * - Politica de sesiuni (STATELESS pentru JWT)
     *
     * @param http - obiect pentru configurare
     * @return SecurityFilterChain configurat
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Dezactivăm CSRF (Cross-Site Request Forgery)
                // Nu e necesar pentru API-uri REST cu JWT
                // CSRF e util doar pentru aplicații cu sesiuni tradiționale
                .csrf(AbstractHttpConfigurer::disable)

                // Configurăm autorizarea request-urilor
                .authorizeHttpRequests(auth -> auth
                        // Endpoint-uri PUBLICE (nu necesită autentificare)
                        .requestMatchers(
                                "/api/auth/**",           // Login, register, reset password
                                "/api/public/**",         // Orice alte endpoint-uri publice
                                "/error",                 // Pagina de eroare
                                "/actuator/health"        // Health check (dacă folosești Spring Actuator)
                        ).permitAll()

                        // Toate celelalte request-uri necesită autentificare
                        .anyRequest().authenticated()
                )

                // Configurăm politica de sesiuni ca STATELESS
                // IMPORTANT pentru JWT: nu creăm sesiuni pe server
                // Fiecare request trebuie să conțină token-ul JWT
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Setăm authentication provider-ul custom
                // Acesta știe cum să valideze userul (folosind UserDetailsService)
                .authenticationProvider(authenticationProvider())

                // Adăugăm JWT filter-ul ÎNAINTEA filter-ului standard de autentificare
                // Ordinea e importantă: vrem să validăm JWT-ul înainte să verificăm autentificarea
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Bean pentru Password Encoder (BCrypt)
     *
     * BCrypt este un algoritm de hashing pentru parole care:
     * - E slow by design (încetinește atacurile brute-force)
     * - Adaugă "salt" automat (fiecare parolă are hash diferit)
     * - E one-way (nu poți decripta hash-ul înapoi la parolă)
     *
     * Exemplu:
     * Parola: "Po12..manager56"
     * Hash BCrypt: "$2a$10$N9qo8uLOickgx2ZMRZoMye.GqvFn8xZGpzTQRqFfEfFfUYYCnmQbC"
     *
     * Același input va produce hash-uri diferite de fiecare dată:
     * "Po12..manager56" → "$2a$10$ABC..."
     * "Po12..manager56" → "$2a$10$XYZ..."
     * Dar BCrypt.matches() va returna true pentru ambele!
     *
     * @return BCryptPasswordEncoder pentru hashing-ul parolelor
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Authentication Provider - componenta care știe cum să valideze credențialele
     *
     * Configurăm un DaoAuthenticationProvider care:
     * 1. Folosește UserDetailsService pentru a încărca userul din DB
     * 2. Folosește PasswordEncoder pentru a compara parola introdusă cu hash-ul
     *
     * Process de validare:
     * 1. User introduce email + parolă
     * 2. UserDetailsService.loadUserByUsername(email) → încarcă user din DB
     * 3. PasswordEncoder.matches(parolaIntrodusă, hashDinDB) → validează
     * 4. Dacă match → autentificare reușită
     * 5. Dacă nu match → InvalidCredentialsException
     *
     * @return AuthenticationProvider configurat
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * Authentication Manager - folosit pentru autentificarea manuală
     *
     * Acest bean e folosit în AuthenticationService la login
     * pentru a autentifica user-ul manual (fără Spring Security auto-config)
     *
     * @param config - configurația de autentificare
     * @return AuthenticationManager
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}

/**
 * NOTĂ IMPORTANTĂ despre ordinea filtrelor:
 *
 * Spring Security are un lanț de filtre care se execută în ordine:
 *
 * 1. JwtAuthenticationFilter (custom - adăugat de noi)
 *    ↓ Extrage și validează JWT, setează autentificarea
 *
 * 2. UsernamePasswordAuthenticationFilter (default Spring Security)
 *    ↓ Procesează form login (nu e relevant pentru noi dacă folosim JWT)
 *
 * 3. FilterSecurityInterceptor (default Spring Security)
 *    ↓ Verifică dacă userul are permisiunile necesare
 *
 * 4. Controller
 *    ↓ Codul nostru de business logic
 *
 * IMPORTANT: JwtAuthenticationFilter trebuie să fie ÎNAINTEA
 * UsernamePasswordAuthenticationFilter pentru a seta autentificarea
 * înainte ca Spring Security să verifice permisiunile.
 */