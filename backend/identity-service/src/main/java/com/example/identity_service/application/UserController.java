package com.example.identity_service.application;

import com.example.identity_service.domain.UserStatus;
import com.example.identity_service.dtos.CreateUserRequest;
import com.example.identity_service.dtos.UpdateProfileRequest;
import com.example.identity_service.dtos.UserResponse;
import com.example.identity_service.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.ppms.shared.security.UserPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@PreAuthorize("@scope.canRead()")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<Page<UserResponse>> list(
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) String q,
            Pageable pageable) {
        return ResponseEntity.ok(userService.listUsers(status, q, pageable));
    }

    @PreAuthorize("@scope.canWrite()")
    @PostMapping
    public ResponseEntity<UserResponse> create(@RequestBody @Valid CreateUserRequest request,
                                               @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(userService.createUser(request, principal != null ? principal.getUserId() : null));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getUser(id));
    }

    @PreAuthorize("@scope.canWrite()")
    @PatchMapping("/{id}")
    public ResponseEntity<UserResponse> update(@PathVariable UUID id, @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    @PreAuthorize("@scope.canWrite()")
    @PostMapping("/{id}/unlock")
    public ResponseEntity<Void> unlock(@PathVariable UUID id) {
        userService.unlockUser(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("@scope.canWrite()")
    @PostMapping("/{id}/disable")
    public ResponseEntity<Void> disable(@PathVariable UUID id) {
        userService.disableUser(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("@scope.canWrite()")
    @PostMapping("/{id}/enable")
    public ResponseEntity<Void> enable(@PathVariable UUID id) {
        userService.enableUser(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("@scope.canWrite()")
    @PostMapping("/{id}/reset-password")
    public ResponseEntity<Void> resetPassword(@PathVariable UUID id) {
        userService.adminResetPassword(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("@scope.canWrite()")
    @PostMapping("/{id}/force-password-change")
    public ResponseEntity<Void> forcePasswordChange(@PathVariable UUID id) {
        userService.forcePasswordChange(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("@scope.canWrite()")
    @PostMapping("/{id}/mfa/reset")
    public ResponseEntity<Void> resetMfa(@PathVariable UUID id) {
        userService.resetMfa(id);
        return ResponseEntity.noContent().build();
    }
}
