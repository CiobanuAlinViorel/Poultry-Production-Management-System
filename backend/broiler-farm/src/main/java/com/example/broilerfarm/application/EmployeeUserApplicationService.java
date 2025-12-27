package com.example.broilerfarm.application;

import com.example.broilerfarm.application.dto.CreateEmployeeUserRequest;
import com.example.broilerfarm.application.dto.UpdateEmployeeUserRequest;
import com.example.broilerfarm.domain.entities.BroilerFarm;
import com.example.broilerfarm.domain.entities.FarmEmployee;
import com.example.broilerfarm.domain.entities.FarmUser;
import com.example.broilerfarm.infrastructure.persistence.repositories.BroilerFarmRepository;
import com.example.broilerfarm.infrastructure.persistence.repositories.FarmEmployeeRepository;
import com.example.broilerfarm.infrastructure.persistence.repositories.FarmUserRepository;
import com.example.shared.domain.enums.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

/**
 * Employee User Application Service
 *
 * Responsabilități:
 * - CRUD pentru FarmEmployee + FarmUser (creare concomitentă)
 * - Generare automată parolă conform algoritmului
 * - Criptare parolă cu BCrypt
 * - Business logic pentru gestionarea angajaților și utilizatorilor
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class EmployeeUserApplicationService {

    private final FarmEmployeeRepository farmEmployeeRepository;
    private final FarmUserRepository farmUserRepository;
    private final BroilerFarmRepository broilerFarmRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Creează un angajat împreună cu utilizatorul asociat
     *
     * Flow:
     * 1. Validează datele de intrare
     * 2. Verifică dacă email-ul există deja
     * 3. Creează FarmEmployee
     * 4. Generează parola automată (Po12..manager56)
     * 5. Criptează parola cu BCrypt
     * 6. Creează FarmUser asociat
     * 7. Salvează ambele entități
     *
     * @param request - datele pentru creare
     * @return FarmUser creat (cu Employee inclus)
     */
    public FarmUser createEmployeeWithUser(CreateEmployeeUserRequest request) {
        log.info("Creating employee and user for email: {}", request.getEmail());

        // 1. Validare
        validateCreateRequest(request);

        // 2. Verifică dacă email-ul există deja
        if (farmUserRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already exists: " + request.getEmail());
        }

        if (farmUserRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username already exists: " + request.getUsername());
        }

        // 3. Găsește ferma
        BroilerFarm farm = broilerFarmRepository.findById(request.getFarmId())
                .orElseThrow(() -> new IllegalArgumentException("Farm not found with id: " + request.getFarmId()));

        // 4. Creează FarmEmployee
        FarmEmployee employee = FarmEmployee.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .role(request.getRole())
                .hireDate(request.getHireDate())
                .broilerFarm(farm)
                .build();

        employee = farmEmployeeRepository.save(employee);
        log.debug("Employee created with id: {}", employee.getId());

        // 5. Generează parola automată
        // Formula: Po12..manager56
        // Po = primele 2 litere din lastName
        // 12 = ziua angajării
        // .. = separator
        // manager = rolul
        // 56 = ultimele 2 cifre din telefon
        String generatedPassword = generatePassword(
                request.getLastName(),
                request.getHireDate(),
                request.getRole(),
                request.getPhone()
        );

        log.debug("Generated password for {}: {}", request.getEmail(), generatedPassword);

        // 6. Criptează parola cu BCrypt
        String passwordHash = passwordEncoder.encode(generatedPassword);

        // 7. Creează FarmUser
        // Creează FarmUser folosind constructorul normal
        FarmUser user = new FarmUser();

// Setează câmpurile moștenite de la User
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordHash);
        user.setIsActive(true);
        user.setAccountLocked(false);
        user.setMustChangePassword(true);
        user.setFailedLoginAttempts(0);
        user.setPasswordLastChanged(LocalDate.now());

// Setează employee-ul asociat
        user.setEmployee(employee);

// Inițializează roles (dacă nu e inițializat deja)
        if (user.getRoles() == null) {
            user.setRoles(new HashSet<>());
        }

// Adaugă rolurile
        user.getRoles().add("EMPLOYEE");
        if (request.getRole() == Role.FARM_MANAGER) {
            user.getRoles().add("MANAGER");
        }

        // Adaugă rolul EMPLOYEE by default
        user.getRoles().add("EMPLOYEE");

        // Dacă e MANAGER, adaugă și rolul de MANAGER
        if (request.getRole() == Role.FARM_MANAGER) {
            user.getRoles().add("MANAGER");
        }

        user = farmUserRepository.save(user);
        log.info("User created successfully with id: {} for employee id: {}", user.getId(), employee.getId());

        return user;
    }

    /**
     * Actualizează datele angajatului și utilizatorului
     *
     * IMPORTANT: Parola NU se schimbă aici - doar prin endpoint dedicat
     *
     * @param employeeId - id-ul angajatului
     * @param request - datele actualizate
     * @return FarmUser actualizat
     */
    public FarmUser updateEmployeeWithUser(Long employeeId, UpdateEmployeeUserRequest request) {
        log.info("Updating employee and user with employeeId: {}", employeeId);

        // 1. Găsește employee-ul
        FarmEmployee employee = farmEmployeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found with id: " + employeeId));

        // 2. Găsește user-ul asociat
        FarmUser user = farmUserRepository.findByEmployee(employee)
                .orElseThrow(() -> new IllegalArgumentException("User not found for employee id: " + employeeId));

        // 3. Update employee
        if (request.getFirstName() != null) {
            employee.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            employee.setLastName(request.getLastName());
        }
        if (request.getPhone() != null) {
            employee.setPhone(request.getPhone());
        }
        if (request.getRole() != null) {
            employee.setRole(request.getRole());

            // Update roles în user
            user.getRoles().clear();
            user.getRoles().add("EMPLOYEE");
            if (request.getRole() == Role.FARM_MANAGER) {
                user.getRoles().add("MANAGER");
            }
        }
        if (request.getFarmId() != null) {
            BroilerFarm farm = broilerFarmRepository.findById(request.getFarmId())
                    .orElseThrow(() -> new IllegalArgumentException("Farm not found with id: " + request.getFarmId()));
            employee.setBroilerFarm(farm);
        }

        // 4. Update user (doar username dacă e furnizat)
        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
            if (farmUserRepository.findByUsername(request.getUsername()).isPresent()) {
                throw new IllegalArgumentException("Username already exists: " + request.getUsername());
            }
            user.setUsername(request.getUsername());
        }

        farmEmployeeRepository.save(employee);
        user = farmUserRepository.save(user);

        log.info("Employee and user updated successfully");
        return user;
    }

    /**
     * Resetează parola unui utilizator
     * Generează o nouă parolă și o criptează
     *
     * @param userId - id-ul userului
     * @return noua parolă generată (înainte de criptare)
     */
    public String resetUserPassword(Long userId) {
        log.info("Resetting password for user id: {}", userId);

        FarmUser user = farmUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        FarmEmployee employee = user.getEmployee();
        if (employee == null) {
            throw new IllegalStateException("User has no associated employee");
        }

        // Generează nouă parolă
        String newPassword = generatePassword(
                employee.getLastName(),
                employee.getHireDate(),
                employee.getRole(),
                employee.getPhone()
        );

        // Criptează și salvează
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(true);
        user.setPasswordLastChanged(LocalDate.now());
        user.setFailedLoginAttempts(0);
        user.setAccountLocked(false);

        farmUserRepository.save(user);
        log.info("Password reset successfully for user id: {}", userId);

        return newPassword;  // Returnează pentru a fi comunicată userului
    }

    /**
     * Schimbă manual parola unui utilizator
     *
     * @param userId - id-ul userului
     * @param newPassword - noua parolă (necriptată)
     */
    public void changeUserPassword(Long userId, String newPassword) {
        log.info("Changing password for user id: {}", userId);

        FarmUser user = farmUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        // Validează parola
        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }

        // Criptează și salvează
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        user.setPasswordLastChanged(LocalDate.now());

        farmUserRepository.save(user);
        log.info("Password changed successfully for user id: {}", userId);
    }

    /**
     * Activează/Dezactivează un utilizator
     */
    public void toggleUserStatus(Long userId, boolean active) {
        log.info("Toggling user status for id: {} to active: {}", userId, active);

        FarmUser user = farmUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        user.setIsActive(active);
        farmUserRepository.save(user);

        log.info("User status updated successfully");
    }

    /**
     * Blochează/Deblochează un utilizator
     */
    public void toggleUserLock(Long userId, boolean locked, String reason) {
        log.info("Toggling user lock for id: {} to locked: {}", userId, locked);

        FarmUser user = farmUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        if (locked) {
            user.lockAccount(reason);
        } else {
            user.unlockAccount();
        }

        farmUserRepository.save(user);
        log.info("User lock status updated successfully");
    }

    /**
     * Găsește user după employee ID
     */
    @Transactional(readOnly = true)
    public Optional<FarmUser> findByEmployeeId(Long employeeId) {
        return farmEmployeeRepository.findById(employeeId)
                .flatMap(farmUserRepository::findByEmployee);
    }

    /**
     * Găsește user după email
     */
    @Transactional(readOnly = true)
    public Optional<FarmUser> findByEmail(String email) {
        return farmUserRepository.findByEmail(email);
    }

    /**
     * Obține toți utilizatorii
     */
    @Transactional(readOnly = true)
    public List<FarmUser> getAllUsers() {
        return farmUserRepository.findAll();
    }

    /**
     * Obține user după ID
     */
    @Transactional(readOnly = true)
    public FarmUser getUserById(Long userId) {
        return farmUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
    }

    /**
     * Șterge un angajat și utilizatorul asociat
     */
    public void deleteEmployeeWithUser(Long employeeId) {
        log.info("Deleting employee and user with employeeId: {}", employeeId);

        FarmEmployee employee = farmEmployeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found with id: " + employeeId));

        // Șterge user-ul asociat (dacă există)
        farmUserRepository.findByEmployee(employee).ifPresent(user -> {
            log.debug("Deleting user id: {}", user.getId());
            farmUserRepository.delete(user);
        });

        // Șterge employee-ul
        farmEmployeeRepository.delete(employee);
        log.info("Employee and user deleted successfully");
    }

    // ========================================
    // HELPER METHODS
    // ========================================

    /**
     * Generează parola automată conform algoritmului
     *
     * Formula: Po12..manager56
     * - Po = primele 2 litere din lastName (uppercase)
     * - 12 = ziua angajării (cu padding 0)
     * - .. = separator fix
     * - manager = rolul (lowercase)
     * - 56 = ultimele 2 cifre din telefon
     */
    private String generatePassword(String lastName, LocalDate hireDate, Role role, String phone) {
        // Primele 2 litere din lastName (uppercase)
        String firstTwoLetters = lastName.substring(0, Math.min(2, lastName.length())).toUpperCase();

        // Ziua angajării (cu padding 0)
        String day = String.format("%02d", hireDate.getDayOfMonth());

        // Separator fix
        String separator = "..";

        // Rolul (lowercase, fără FARM_ prefix)
        String roleStr = role.toString().toLowerCase().replace("farm_", "");

        // Ultimele 2 cifre din telefon
        String phoneDigits = phone.replaceAll("\\D", ""); // Elimină tot ce nu e digit
        String lastTwoDigits = phoneDigits.substring(Math.max(0, phoneDigits.length() - 2));

        return firstTwoLetters + day + separator + roleStr + lastTwoDigits;
    }

    /**
     * Validează request-ul de creare
     */
    private void validateCreateRequest(CreateEmployeeUserRequest request) {
        if (request.getFirstName() == null || request.getFirstName().trim().isEmpty()) {
            throw new IllegalArgumentException("First name is required");
        }
        if (request.getLastName() == null || request.getLastName().trim().isEmpty()) {
            throw new IllegalArgumentException("Last name is required");
        }
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (request.getPhone() == null || request.getPhone().trim().isEmpty()) {
            throw new IllegalArgumentException("Phone is required");
        }
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (request.getRole() == null) {
            throw new IllegalArgumentException("Role is required");
        }
        if (request.getHireDate() == null) {
            throw new IllegalArgumentException("Hire date is required");
        }
        if (request.getFarmId() == null) {
            throw new IllegalArgumentException("Farm ID is required");
        }
    }
}



