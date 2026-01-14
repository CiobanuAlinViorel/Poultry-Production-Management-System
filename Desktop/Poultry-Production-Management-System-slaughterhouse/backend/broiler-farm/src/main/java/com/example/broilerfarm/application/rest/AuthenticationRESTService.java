package com.example.broilerfarm.application.rest;

import com.example.broilerfarm.application.AuthenticationApplicationService;
import com.example.broilerfarm.application.dto.ErrorResponse;
import com.example.broilerfarm.application.dto.MessageResponse;
import com.example.broilerfarm.application.dto.ValidationResponse;
import com.example.broilerfarm.domain.exceptions.InvalidCredentialsException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.security.auth.login.AccountLockedException;

/**
 * Authentication Controller
 *
 * Endpoint-uri pentru autentificare:
 * - POST /api/auth/login - autentificare user
 * - POST /api/auth/logout - deconectare (opțional)
 *
 * Aceste endpoint-uri sunt PUBLICE (definite în SecurityConfiguration)
 * Nu necesită JWT token pentru acces
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationRESTService {

    private final AuthenticationApplicationService authenticationService;

    /**
     * Endpoint pentru login
     *
     * Request:
     * POST /api/auth/login
     * {
     *   "email": "john@example.com",
     *   "password": "Po12..manager56"
     * }
     *
     * Response Success (200 OK):
     * {
     *   "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqb2huQGV4YW1wbGUuY29tIiwicm9sZXMiOlsiRU1QTE9ZRUUiXSwiaWF0IjoxNjk...",
     *   "email": "john@example.com",
     *   "username": "john_doe",
     *   "roles": ["EMPLOYEE"]
     * }
     *
     * Response Error (401 Unauthorized):
     * {
     *   "error": "Invalid email or password"
     * }
     *
     * Response Error (403 Forbidden):
     * {
     *   "error": "Account is locked: Maximum login attempts exceeded"
     * }
     *
     * @param request - LoginRequest cu email și parolă
     * @return AuthenticationResponse cu token JWT
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody com.example.broilerfarm.application.dto.LoginRequest request) {
        try {
            var response = authenticationService.login(request);
            return ResponseEntity.ok(response);
        } catch (InvalidCredentialsException e) {
            return ResponseEntity.status(401)
                    .body(new ErrorResponse("Invalid email or password"));
        } catch (AccountLockedException e) {
            return ResponseEntity.status(403)
                    .body(new ErrorResponse("Account is locked: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ErrorResponse("An error occurred during authentication"));
        }
    }

    /**
     * Endpoint pentru logout (opțional)
     *
     * În cazul JWT, logout-ul se face de obicei client-side
     * (clientul șterge token-ul din localStorage)
     *
     * Request:
     * POST /api/auth/logout
     * Header: Authorization: Bearer <token>
     *
     * Response (200 OK):
     * {
     *   "message": "Logged out successfully"
     * }
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            authenticationService.logout(token);
        }
        return ResponseEntity.ok(new MessageResponse("Logged out successfully"));
    }

    /**
     * Endpoint pentru verificarea validității unui token
     * Util pentru front-end să verifice dacă token-ul mai e valid
     *
     * Request:
     * GET /api/auth/validate
     * Header: Authorization: Bearer <token>
     *
     * Response (200 OK):
     * {
     *   "valid": true,
     *   "email": "john@example.com"
     * }
     */
    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401)
                    .body(new ErrorResponse("No token provided"));
        }

        String token = authHeader.substring(7);
        String email = authenticationService.extractEmail(token);

        boolean isValid = authenticationService.validateToken(token, email);

        return ResponseEntity.ok(new ValidationResponse(isValid, email));
    }
}

/**
 * DTOs pentru Request și Response
 */








/**
 * IMPORTANT: Nu uita să adaugi InvalidCredentialsException în package-ul de excepții
 * Sau poți folosi direct exceptia din shared.domain.exception
 */


/**
 * EXEMPLE DE FOLOSIRE DIN FRONT-END:
 *
 * // 1. Login
 * const login = async (email, password) => {
 *   const response = await fetch('/api/auth/login', {
 *     method: 'POST',
 *     headers: { 'Content-Type': 'application/json' },
 *     body: JSON.stringify({ email, password })
 *   });
 *
 *   const data = await response.json();
 *
 *   if (response.ok) {
 *     // Salvează token-ul în localStorage
 *     localStorage.setItem('token', data.token);
 *     return data;
 *   } else {
 *     throw new Error(data.error);
 *   }
 * };
 *
 * // 2. Request autentificat
 * const getFarms = async () => {
 *   const token = localStorage.getItem('token');
 *
 *   const response = await fetch('/api/farms', {
 *     headers: {
 *       'Authorization': `Bearer ${token}`
 *     }
 *   });
 *
 *   return await response.json();
 * };
 *
 * // 3. Logout
 * const logout = () => {
 *   localStorage.removeItem('token');
 *   // Redirect to login page
 * };
 */