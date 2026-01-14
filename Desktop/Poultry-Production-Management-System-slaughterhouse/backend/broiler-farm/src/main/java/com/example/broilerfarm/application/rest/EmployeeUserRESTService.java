package com.example.broilerfarm.application.rest;

import com.example.broilerfarm.application.EmployeeUserApplicationService;
import com.example.broilerfarm.application.dto.CreateEmployeeUserRequest;
import com.example.broilerfarm.application.dto.ErrorResponse;
import com.example.broilerfarm.application.dto.MessageResponse;
import com.example.broilerfarm.application.dto.UpdateEmployeeUserRequest;
import com.example.broilerfarm.application.dto.create_employee_user.*;
import com.example.broilerfarm.domain.entities.FarmUser;
import com.example.shared.domain.enums.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Employee User REST Controller
 *
 * Endpoint-uri pentru gestionarea angajaților și utilizatorilor:
 * - POST   /api/employees - Creare angajat + user
 * - GET    /api/employees - Lista tuturor utilizatorilor
 * - GET    /api/employees/{id} - Detalii user
 * - PUT    /api/employees/{id} - Actualizare angajat + user
 * - DELETE /api/employees/{id} - Ștergere angajat + user
 * - POST   /api/employees/{id}/reset-password - Reset parolă
 * - PUT    /api/employees/{id}/change-password - Schimbare parolă
 * - PUT    /api/employees/{id}/toggle-status - Activare/Dezactivare
 * - PUT    /api/employees/{id}/toggle-lock - Blocare/Deblocare
 */
@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
@Slf4j
public class EmployeeUserRESTService {

    private final EmployeeUserApplicationService employeeUserService;

    /**
     * Creare angajat și utilizator
     *
     * POST /api/employees
     * {
     *   "firstName": "John",
     *   "lastName": "Popescu",
     *   "email": "john@example.com",
     *   "phone": "0740123456",
     *   "username": "john_popescu",
     *   "role": "FARM_MANAGER",
     *   "hireDate": "2023-10-12",
     *   "farmId": 1
     * }
     *
     * Response 201 Created:
     * {
     *   "id": 1,
     *   "username": "john_popescu",
     *   "email": "john@example.com",
     *   "isActive": true,
     *   "accountLocked": false,
     *   "roles": ["EMPLOYEE", "MANAGER"],
     *   "employee": {
     *     "id": 1,
     *     "firstName": "John",
     *     "lastName": "Popescu",
     *     "email": "john@example.com",
     *     "phone": "0740123456",
     *     "role": "FARM_MANAGER",
     *     "hireDate": "2023-10-12"
     *   },
     *   "generatedPassword": "Po12..manager56"
     * }
     */
    @PostMapping
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<?> createEmployeeWithUser(@RequestBody CreateEmployeeUserDTO dto) {
        try {
            log.info("Creating employee and user: {}", dto.getEmail());

            FarmUser user = employeeUserService.createEmployeeWithUser(
                    convertToCreateRequest(dto)
            );

            // Generează password-ul pentru răspuns (îl refacem pentru a-l arăta userului)
            String generatedPassword = generatePasswordForResponse(
                    dto.getLastName(),
                    dto.getHireDate(),
                    dto.getRole(),
                    dto.getPhone()
            );

            EmployeeUserResponseDTO response = convertToResponseDTO(user);
            response.setGeneratedPassword(generatedPassword);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            log.error("Validation error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating employee and user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Error creating employee: " + e.getMessage()));
        }
    }

    /**
     * Obține toți utilizatorii
     *
     * GET /api/employees
     *
     * Response 200 OK:
     * [
     *   {
     *     "id": 1,
     *     "username": "john_popescu",
     *     "email": "john@example.com",
     *     "isActive": true,
     *     "accountLocked": false,
     *     "roles": ["EMPLOYEE", "MANAGER"],
     *     "employee": { ... }
     *   }
     * ]
     */
    @GetMapping
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<List<EmployeeUserResponseDTO>> getAllUsers() {
        log.info("Fetching all users");

        List<FarmUser> users = employeeUserService.getAllUsers();
        List<EmployeeUserResponseDTO> response = users.stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Obține detalii user după ID
     *
     * GET /api/employees/{id}
     *
     * Response 200 OK: { ... detalii user ... }
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        try {
            log.info("Fetching user with id: {}", id);

            FarmUser user = employeeUserService.getUserById(id);
            return ResponseEntity.ok(convertToResponseDTO(user));

        } catch (IllegalArgumentException e) {
            log.error("User not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Actualizează angajat și utilizator
     *
     * PUT /api/employees/{id}
     * {
     *   "firstName": "John Updated",
     *   "lastName": "Popescu",
     *   "phone": "0740999999",
     *   "username": "john_updated",
     *   "role": "EMPLOYEE",
     *   "farmId": 2
     * }
     *
     * Response 200 OK: { ... user actualizat ... }
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<?> updateEmployeeWithUser(
            @PathVariable Long id,
            @RequestBody UpdateEmployeeUserDTO dto) {
        try {
            log.info("Updating employee with id: {}", id);

            // Găsim user-ul pentru a obține employeeId
            FarmUser currentUser = employeeUserService.getUserById(id);
            if (currentUser.getEmployee() == null) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("User has no associated employee"));
            }

            Long employeeId = currentUser.getEmployee().getId();

            FarmUser updatedUser = employeeUserService.updateEmployeeWithUser(
                    employeeId,
                    convertToUpdateRequest(dto)
            );

            return ResponseEntity.ok(convertToResponseDTO(updatedUser));

        } catch (IllegalArgumentException e) {
            log.error("Validation error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating employee", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Error updating employee: " + e.getMessage()));
        }
    }

    /**
     * Șterge angajat și utilizator
     *
     * DELETE /api/employees/{id}
     *
     * Response 204 No Content
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteEmployeeWithUser(@PathVariable Long id) {
        try {
            log.info("Deleting employee with id: {}", id);

            // Găsim user-ul pentru a obține employeeId
            FarmUser user = employeeUserService.getUserById(id);
            if (user.getEmployee() == null) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("User has no associated employee"));
            }

            Long employeeId = user.getEmployee().getId();
            employeeUserService.deleteEmployeeWithUser(employeeId);

            return ResponseEntity.noContent().build();

        } catch (IllegalArgumentException e) {
            log.error("Employee not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Reset parolă utilizator
     *
     * POST /api/employees/{id}/reset-password
     *
     * Response 200 OK:
     * {
     *   "message": "Password reset successfully",
     *   "newPassword": "Po12..manager56"
     * }
     */
    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<?> resetPassword(@PathVariable Long id) {
        try {
            log.info("Resetting password for user id: {}", id);

            String newPassword = employeeUserService.resetUserPassword(id);

            return ResponseEntity.ok(new PasswordResetResponse(
                    "Password reset successfully",
                    newPassword
            ));

        } catch (IllegalArgumentException e) {
            log.error("User not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Schimbă manual parola
     *
     * PUT /api/employees/{id}/change-password
     * {
     *   "newPassword": "NewSecurePassword123!"
     * }
     *
     * Response 200 OK:
     * {
     *   "message": "Password changed successfully"
     * }
     */
    @PutMapping("/{id}/change-password")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<?> changePassword(
            @PathVariable Long id,
            @RequestBody ChangePasswordDTO dto) {
        try {
            log.info("Changing password for user id: {}", id);

            employeeUserService.changeUserPassword(id, dto.getNewPassword());

            return ResponseEntity.ok(new MessageResponse("Password changed successfully"));

        } catch (IllegalArgumentException e) {
            log.error("Validation error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Activează/Dezactivează utilizator
     *
     * PUT /api/employees/{id}/toggle-status
     * {
     *   "active": false
     * }
     *
     * Response 200 OK:
     * {
     *   "message": "User status updated successfully"
     * }
     */
    @PutMapping("/{id}/toggle-status")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<?> toggleUserStatus(
            @PathVariable Long id,
            @RequestBody ToggleStatusDTO dto) {
        try {
            log.info("Toggling status for user id: {} to active: {}", id, dto.isActive());

            employeeUserService.toggleUserStatus(id, dto.isActive());

            return ResponseEntity.ok(new MessageResponse("User status updated successfully"));

        } catch (IllegalArgumentException e) {
            log.error("User not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Blochează/Deblochează utilizator
     *
     * PUT /api/employees/{id}/toggle-lock
     * {
     *   "locked": true,
     *   "reason": "Suspicious activity detected"
     * }
     *
     * Response 200 OK:
     * {
     *   "message": "User lock status updated successfully"
     * }
     */
    @PutMapping("/{id}/toggle-lock")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<?> toggleUserLock(
            @PathVariable Long id,
            @RequestBody ToggleLockDTO dto) {
        try {
            log.info("Toggling lock for user id: {} to locked: {}", id, dto.isLocked());

            employeeUserService.toggleUserLock(id, dto.isLocked(), dto.getReason());

            return ResponseEntity.ok(new MessageResponse("User lock status updated successfully"));

        } catch (IllegalArgumentException e) {
            log.error("User not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    // ========================================
    // HELPER METHODS
    // ========================================

    private CreateEmployeeUserRequest convertToCreateRequest(CreateEmployeeUserDTO dto) {
        return CreateEmployeeUserRequest.builder()
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .username(dto.getUsername())
                .role(dto.getRole())
                .hireDate(dto.getHireDate())
                .farmId(dto.getFarmId())
                .build();
    }

    private UpdateEmployeeUserRequest convertToUpdateRequest(UpdateEmployeeUserDTO dto) {
        return UpdateEmployeeUserRequest.builder()
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .phone(dto.getPhone())
                .username(dto.getUsername())
                .role(dto.getRole())
                .farmId(dto.getFarmId())
                .build();
    }

    private EmployeeUserResponseDTO convertToResponseDTO(FarmUser user) {
        EmployeeUserResponseDTO dto = new EmployeeUserResponseDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setIsActive(user.getIsActive());
        dto.setAccountLocked(user.getAccountLocked());
        dto.setRoles(user.getRoles());
        dto.setLastLoginDate(user.getLastLoginDate());
        dto.setMustChangePassword(user.getMustChangePassword());

        if (user.getEmployee() != null) {
            EmployeeDTO employeeDTO = new EmployeeDTO();
            employeeDTO.setId(user.getEmployee().getId());
            employeeDTO.setFirstName(user.getEmployee().getFirstName());
            employeeDTO.setLastName(user.getEmployee().getLastName());
            employeeDTO.setEmail(user.getEmployee().getEmail());
            employeeDTO.setPhone(user.getEmployee().getPhone());
            employeeDTO.setRole(user.getEmployee().getRole());
            employeeDTO.setHireDate(user.getEmployee().getHireDate());

            dto.setEmployee(employeeDTO);
        }

        return dto;
    }

    private String generatePasswordForResponse(String lastName, LocalDate hireDate, Role role, String phone) {
        String firstTwoLetters = lastName.substring(0, Math.min(2, lastName.length())).toUpperCase();
        String day = String.format("%02d", hireDate.getDayOfMonth());
        String separator = "..";
        String roleStr = role.toString().toLowerCase().replace("farm_", "");
        String phoneDigits = phone.replaceAll("\\D", "");
        String lastTwoDigits = phoneDigits.substring(Math.max(0, phoneDigits.length() - 2));

        return firstTwoLetters + day + separator + roleStr + lastTwoDigits;
    }
}

// ========================================
// DTOs
// ========================================

