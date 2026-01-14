package com.example.broilerfarm.config;


import com.example.broilerfarm.services.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT Authentication Filter
 *
 * Responsabilități:
 * - Interceptează FIECARE request HTTP
 * - Extrage token-ul JWT din header-ul Authorization
 * - Validează token-ul
 * - Setează autentificarea în SecurityContext pentru request-ul curent
 *
 * Logica din spate:
 * Acest filter se execută ÎNAINTEA tuturor controller-elor și funcționează astfel:
 *
 * FLOW-UL FILTRULUI:
 * ┌─────────────────────────────────────────────────────────────┐
 * │ Client face request → GET /api/farms                        │
 * │ Header: Authorization: Bearer eyJhbGci...                   │
 * └────────────────┬────────────────────────────────────────────┘
 *                  ▼
 * ┌─────────────────────────────────────────────────────────────┐
 * │ 1. JwtAuthenticationFilter interceptează request-ul        │
 * └────────────────┬────────────────────────────────────────────┘
 *                  ▼
 * ┌─────────────────────────────────────────────────────────────┐
 * │ 2. Extrage header-ul "Authorization"                        │
 * │    - Dacă nu există → trece request mai departe (public)    │
 * │    - Dacă există → verifică dacă începe cu "Bearer "        │
 * └────────────────┬────────────────────────────────────────────┘
 *                  ▼
 * ┌─────────────────────────────────────────────────────────────┐
 * │ 3. Extrage token-ul (fără "Bearer ")                        │
 * │    Token: eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOi...                │
 * └────────────────┬────────────────────────────────────────────┘
 *                  ▼
 * ┌─────────────────────────────────────────────────────────────┐
 * │ 4. Extrage email-ul din token folosind JwtService          │
 * └────────────────┬────────────────────────────────────────────┘
 *                  ▼
 * ┌─────────────────────────────────────────────────────────────┐
 * │ 5. Dacă nu există autentificare în SecurityContext         │
 * │    → Încarcă detaliile userului din DB                      │
 * └────────────────┬────────────────────────────────────────────┘
 *                  ▼
 * ┌─────────────────────────────────────────────────────────────┐
 * │ 6. Validează token-ul                                       │
 * │    - Verifică dacă email-ul din token = email user          │
 * │    - Verifică dacă token-ul nu a expirat                    │
 * └────────────────┬────────────────────────────────────────────┘
 *                  ▼
 * ┌─────────────────────────────────────────────────────────────┐
 * │ 7. Dacă token valid → Setează autentificarea                │
 * │    - Creează UsernamePasswordAuthenticationToken            │
 * │    - Include authorities (roluri) ale userului              │
 * │    - Setează în SecurityContextHolder                       │
 * └────────────────┬────────────────────────────────────────────┘
 *                  ▼
 * ┌─────────────────────────────────────────────────────────────┐
 * │ 8. Request-ul merge mai departe la Controller               │
 * │    - Controller-ul are acces la user autentificat           │
 * │    - Poate folosi @PreAuthorize pentru verificare roluri    │
 * └─────────────────────────────────────────────────────────────┘
 *
 * OncePerRequestFilter = se execută o singură dată per request
 * (chiar dacă request-ul e forwardat intern)
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final TokenBlacklistService tokenBlacklistService;  // Adaugă blacklist service

    /**
     * Metoda principală care interceptează fiecare request
     *
     * @param request - HTTP request
     * @param response - HTTP response
     * @param filterChain - lanțul de filtre (pentru a continua procesarea)
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. Extragem header-ul Authorization
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        // 2. Verificăm dacă header-ul există și începe cu "Bearer "
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // Nu există token → lăsăm request-ul să continue
            // Poate fi un endpoint public (login, register)
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Extragem token-ul (eliminăm "Bearer " din față)
        //    "Bearer eyJhbGci..." → "eyJhbGci..."
        jwt = authHeader.substring(7);

        // 3.1 Verificăm dacă token-ul e în blacklist (invalidat la logout)
        if (tokenBlacklistService.isBlacklisted(jwt)) {
            logger.info("Token is blacklisted (user logged out)");
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 4. Extragem email-ul (username) din token
            userEmail = jwtService.extractUsername(jwt);

            // 5. Verificăm dacă userul nu este deja autentificat
            //    SecurityContextHolder.getContext().getAuthentication() == null
            //    înseamnă că nu există autentificare pentru request-ul curent
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // 6. Încărcăm detaliile userului din baza de date
                //    UserDetailsService e implementat de CustomUserDetailsService
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                // 7. Validăm token-ul
                if (jwtService.isTokenValid(jwt, userDetails.getUsername())) {

                    // 8. Extragem rolurile din token (dacă există)
                    List<String> roles = jwtService.extractRoles(jwt);
                    List<SimpleGrantedAuthority> authorities = null;

                    if (roles != null) {
                        // Convertim rolurile în authorities pentru Spring Security
                        // "EMPLOYEE" → SimpleGrantedAuthority("ROLE_EMPLOYEE")
                        authorities = roles.stream()
                                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                                .collect(Collectors.toList());
                    }

                    // 9. Creăm obiectul de autentificare
                    //    Acest obiect spune Spring Security: "userul este autentificat"
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,           // Principal (userul autentificat)
                            null,                  // Credentials (nu mai avem nevoie de parolă după autentificare)
                            authorities != null ? authorities : userDetails.getAuthorities()  // Roluri/Permissions
                    );

                    // 10. Adăugăm detalii suplimentare despre request (IP, session, etc.)
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    // 11. Setăm autentificarea în SecurityContext
                    //     De acum, acest request este considerat autentificat
                    //     Controller-ul poate accesa userul cu:
                    //     @AuthenticationPrincipal UserDetails user
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Token invalid, expirat sau corupt
            // Logăm eroarea și continuăm fără autentificare
            logger.error("JWT authentication failed: " + e.getMessage());
        }

        // 12. Continuăm lanțul de filtre către controller
        filterChain.doFilter(request, response);
    }
}