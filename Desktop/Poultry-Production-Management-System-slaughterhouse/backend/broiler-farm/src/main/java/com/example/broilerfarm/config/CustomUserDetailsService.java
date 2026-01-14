package com.example.broilerfarm.config;

import com.example.broilerfarm.domain.entities.FarmUser;

import com.example.broilerfarm.infrastructure.persistence.repositories.FarmUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

/**
 * Custom UserDetailsService Implementation
 *
 * Responsabilități:
 * - Încarcă detaliile userului din baza de date
 * - Convertește FarmUser entity în UserDetails (interfață Spring Security)
 * - Este folosit de Spring Security pentru validarea credențialelor
 *
 * Logica din spate:
 * Spring Security lucrează cu interfața UserDetails, nu cu entitățile noastre.
 * Acest service face "bridge" între FarmUser (domain entity) și UserDetails (Spring Security).
 *
 * CÂND SE APELEAZĂ ACEST SERVICE:
 *
 * 1. La Login (prin AuthenticationProvider):
 *    User → email + password
 *         ↓
 *    loadUserByUsername(email) → FarmUser din DB
 *         ↓
 *    Convert FarmUser → UserDetails
 *         ↓
 *    PasswordEncoder validează parola
 *         ↓
 *    Dacă OK → generează JWT token
 *
 * 2. La fiecare Request autentificat (prin JwtAuthenticationFilter):
 *    Token JWT în header
 *         ↓
 *    Extract email din token
 *         ↓
 *    loadUserByUsername(email) → FarmUser din DB
 *         ↓
 *    Validează token cu userul din DB
 *         ↓
 *    Setează autentificarea în SecurityContext
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final FarmUserRepository farmUserRepository;

    /**
     * Încarcă un user din baza de date după username (în cazul nostru, email)
     *
     * @param username - în cazul nostru, acesta este email-ul
     * @return UserDetails - obiect Spring Security cu detaliile userului
     * @throws UsernameNotFoundException - dacă userul nu există
     *
     * PROCESUL DE CONVERSIE FarmUser → UserDetails:
     *
     * FarmUser (domain entity):
     * - email: "john@example.com"
     * - passwordHash: "$2a$10$..."
     * - roles: ["EMPLOYEE", "MANAGER"]
     * - isActive: true
     * - accountLocked: false
     *
     * UserDetails (Spring Security):
     * - username: "john@example.com"
     * - password: "$2a$10$..."
     * - authorities: [ROLE_EMPLOYEE, ROLE_MANAGER]
     * - enabled: true
     * - accountNonLocked: true
     * - accountNonExpired: true
     * - credentialsNonExpired: true
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        // 1. Căutăm userul în baza de date după email
        FarmUser user = farmUserRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with email: " + username
                ));

        // 2. Convertim rolurile din Set<String> în List<SimpleGrantedAuthority>
        //    Spring Security lucrează cu GrantedAuthority pentru roluri
        //    Rolurile trebuie să înceapă cu "ROLE_" pentru @PreAuthorize
        //
        //    Exemplu:
        //    user.getRoles() = ["EMPLOYEE", "MANAGER"]
        //    authorities = [SimpleGrantedAuthority("ROLE_EMPLOYEE"),
        //                   SimpleGrantedAuthority("ROLE_MANAGER")]
        var authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());

        // 3. Construim și returnăm obiectul UserDetails
        //    Folosim builder-ul din Spring Security
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())           // Username-ul (în cazul nostru, email)
                .password(user.getPasswordHash())    // Hash-ul parolei (BCrypt)
                .authorities(authorities)            // Rolurile convertite în authorities
                .accountExpired(!user.isAccountActive())          // Contul nu e expirat dacă e activ
                .accountLocked(user.isAccountLocked())            // Dacă contul e blocat
                .credentialsExpired(user.hasExpiredPassword())    // Dacă parola e expirată
                .disabled(!user.isAccountActive())                // Dacă contul e dezactivat
                .build();
    }

    /**
     * Metodă helper pentru a încărca user-ul ca FarmUser (nu UserDetails)
     * Utilă când ai nevoie de entitatea completă, nu doar pentru autentificare
     *
     * @param email - email-ul userului
     * @return FarmUser entity
     */
    public FarmUser loadFarmUserByEmail(String email) {
        return farmUserRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with email: " + email
                ));
    }
}

/**
 * EXEMPLE DE FOLOSIRE:
 *
 * 1. În Controller, pentru a obține userul autentificat:
 *
 * @GetMapping("/profile")
 * public ResponseEntity<UserProfile> getProfile(
 *         @AuthenticationPrincipal UserDetails userDetails) {
 *
 *     // userDetails.getUsername() → email-ul userului
 *     // userDetails.getAuthorities() → rolurile
 *
 *     FarmUser user = customUserDetailsService.loadFarmUserByEmail(
 *         userDetails.getUsername()
 *     );
 *
 *     return ResponseEntity.ok(convertToDto(user));
 * }
 *
 * 2. Cu @PreAuthorize pentru verificare roluri:
 *
 * @PreAuthorize("hasRole('MANAGER')")
 * @PostMapping("/farms")
 * public ResponseEntity<Farm> createFarm(@RequestBody FarmDto dto) {
 *     // Doar userii cu rol MANAGER pot accesa
 * }
 *
 * 3. Verificare manuală în service:
 *
 * Authentication auth = SecurityContextHolder.getContext().getAuthentication();
 * String email = auth.getName();
 * FarmUser user = customUserDetailsService.loadFarmUserByEmail(email);
 */