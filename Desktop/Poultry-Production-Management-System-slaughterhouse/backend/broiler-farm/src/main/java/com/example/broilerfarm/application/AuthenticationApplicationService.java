package com.example.broilerfarm.application;

import com.example.broilerfarm.application.dto.AuthenticationResponse;
import com.example.broilerfarm.application.dto.LoginRequest;
import com.example.broilerfarm.config.TokenBlacklistService;
import com.example.broilerfarm.domain.entities.FarmEmployee;
import com.example.broilerfarm.domain.entities.FarmUser;

import com.example.broilerfarm.infrastructure.persistence.repositories.FarmUserRepository;
import com.example.broilerfarm.services.JwtService;
import com.example.shared.domain.exception.InvalidCredentialsException;

import com.example.shared.domain.valueobject.Password;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.security.auth.login.AccountLockedException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Authentication Service - Application Level
 *
 * Responsabilități:
 * - Orchestrează procesul de autentificare (login)
 * - Coordonează între domain entities, repositories și JWT service
 * - Gestionează logout (invalidare token - opțional)
 *
 * Logica din spate:
 * Acest service acționează ca "dirijor" care coordonează mai multe componente:
 * 1. FarmUserRepository - pentru găsirea userului în DB
 * 2. User entity - pentru validarea credențialelor (domain logic)
 * 3. JwtService - pentru generarea token-ului
 * 4. PasswordEncoder - pentru compararea parolelor hash-uite
 */
@Service
@RequiredArgsConstructor
public class AuthenticationApplicationService {

    private final FarmUserRepository farmUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;

    /**
     * Autentifică un utilizator și returnează un token JWT
     *
     * @param request - conține email și parola
     * @return AuthenticationResponse cu token-ul JWT și detalii user
     * @throws InvalidCredentialsException - dacă credențialele sunt greșite
     * @throws AccountLockedException - dacă contul e blocat
     *
     * FLOW-UL DE AUTENTIFICARE:
     * ┌─────────────────────────────────────────────────────────────┐
     * │ 1. Client trimite email + password                          │
     * └────────────────┬────────────────────────────────────────────┘
     *                  ▼
     * ┌─────────────────────────────────────────────────────────────┐
     * │ 2. Căutăm userul în DB după email                           │
     * │    - Dacă nu există → InvalidCredentialsException           │
     * └────────────────┬────────────────────────────────────────────┘
     *                  ▼
     * ┌─────────────────────────────────────────────────────────────┐
     * │ 3. Validăm credențialele prin User entity (domain logic)    │
     * │    - Verifică dacă contul e activ                           │
     * │    - Verifică dacă contul e blocat                          │
     * │    - Compară parola cu hash-ul din DB                       │
     * │    - Incrementează failed attempts dacă greșit              │
     * │    - Blochează contul după 5 încercări                      │
     * └────────────────┬────────────────────────────────────────────┘
     *                  ▼
     * ┌─────────────────────────────────────────────────────────────┐
     * │ 4. Dacă tot OK, generăm token JWT                           │
     * │    - Includem username (email)                              │
     * │    - Includem rolurile userului                             │
     * │    - Token valid 24h                                        │
     * └────────────────┬────────────────────────────────────────────┘
     *                  ▼
     * ┌─────────────────────────────────────────────────────────────┐
     * │ 5. Înregistrăm login-ul reușit                              │
     * │    - Reset failed attempts la 0                             │
     * │    - Setăm lastLoginDate                                    │
     * │    - Deblocăm contul (dacă era blocat temporar)             │
     * └────────────────┬────────────────────────────────────────────┘
     *                  ▼
     * ┌─────────────────────────────────────────────────────────────┐
     * │ 6. Returnăm token-ul către client                           │
     * └─────────────────────────────────────────────────────────────┘
     */
    @Transactional
    public AuthenticationResponse login(LoginRequest request) throws AccountLockedException {
        // 1. Găsim userul după email
        FarmUser user = farmUserRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        // 2. Validăm credențialele folosind domain logic din User entity
        //    Aceasta va:
        //    - Verifica dacă contul e activ și nu e blocat
        //    - Compara parola cu BCrypt
        //    - Incrementa failed attempts dacă e greșit
        //    - Bloca contul după 5 încercări
        // Verifică parola direct cu PasswordEncoder (fără validare strength)

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            // Parola greșită - înregistrează failed attempt
            user.recordFailedLoginAttempt();
            farmUserRepository.save(user);
            throw new InvalidCredentialsException("Invalid email or password");
        }

        // Parola corectă - înregistrează login reușit
        user.recordSuccessfulLogin();

        // 3. Dacă am ajuns aici, autentificarea a reușit
        //    Salvăm userul pentru a persista lastLoginDate și reset failed attempts
        farmUserRepository.save(user);


        // 4. Generăm token JWT cu rolurile userului
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("roles", user.getRoles());

        String jwtToken = jwtService.generateToken(extraClaims, user.getEmail());

        // 5. Returnăm răspunsul cu token-ul
        FarmEmployee employee = user.getEmployee();

        return AuthenticationResponse.builder()
                .token(jwtToken)
                .email(user.getEmail())
                .username(user.getUsername())
                .roles(new ArrayList<>(user.getRoles()))
                .employeeId(employee != null ? employee.getId() : null)
                .farmId(employee != null && employee.getBroilerFarm() != null
                        ? employee.getBroilerFarm().getId() : null)
                .build();
    }

    /**
     * Logout (invalidare token prin blacklist)
     *
     * Adaugă token-ul în blacklist pentru a-l invalida instant.
     * Token-ul nu va mai putea fi folosit până la expirare.
     *
     * @param token - token-ul JWT de invalidat
     */
    public void logout(String token) {
        if (token != null && !token.isEmpty()) {
            tokenBlacklistService.blacklistToken(token);

        }
    }


    /**
     * Verifică dacă un token este valid
     *
     * @param token - token-ul de verificat
     * @param email - email-ul userului
     * @return true dacă token-ul e valid
     */
    public boolean validateToken(String token, String email) {
        return jwtService.isTokenValid(token, email);
    }

    /**
     * Extrage email-ul din token
     *
     * @param token - token JWT
     * @return email-ul userului
     */
    public String extractEmail(String token) {
        return jwtService.extractUsername(token);
    }
}