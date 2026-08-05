package com.inventra.auth;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // ── Authentication ──────────────────────────────────────────────────────

    @PostMapping("/auth/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // ── OTP Login ─────────────────────────────────────────────────────────

    /** Step 1: Request OTP — sends 6-digit code to registered email */
    @PostMapping("/auth/otp/request")
    public ResponseEntity<Map<String, String>> requestOtp(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email is required."));
        }
        return ResponseEntity.ok(authService.requestOtp(email.trim()));
    }

    /** Step 2: Verify OTP — returns JWT on success */
    @PostMapping("/auth/otp/verify")
    public ResponseEntity<AuthResponse> verifyOtp(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String otp   = body.get("otp");
        if (email == null || otp == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(authService.verifyOtpLogin(email.trim(), otp.trim()));
    }

    @PostMapping("/auth/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/auth/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email is required."));
        }
        return ResponseEntity.ok(authService.forgotPassword(email.trim()));
    }

    @PostMapping("/auth/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody Map<String, String> body) {
        String token       = body.get("token");
        String newPassword = body.get("newPassword");
        if (token == null || newPassword == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Token and new password are required."));
        }
        return ResponseEntity.ok(authService.resetPassword(token, newPassword));
    }

    @PatchMapping("/auth/profile")
    public ResponseEntity<UserResponse> updateProfile(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(authService.updateProfile(
            currentUser,
            body.get("name"),
            body.get("phone"),
            body.get("address"),
            body.get("department")
        ));
    }

    @PatchMapping("/auth/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User currentUser) {
        String current    = body.get("currentPassword");
        String newPwd     = body.get("newPassword");
        if (current == null || newPwd == null || newPwd.length() < 6) {
            return ResponseEntity.badRequest()
                .body(Map.of("message", "Both passwords required. New password min 6 characters."));
        }
        return ResponseEntity.ok(authService.changePassword(currentUser, current, newPwd));
    }

    /** User deactivates their own account */
    @DeleteMapping("/auth/account")
    public ResponseEntity<Map<String, String>> deleteOwnAccount(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(authService.deactivateOwnAccount(currentUser));
    }

    // ── User Management ─────────────────────────────────────────────────────

    @GetMapping("/users")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(authService.getAllUsers());
    }

    @PostMapping("/users")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<UserResponse> createUser(
            @RequestBody CreateUserRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.createUser(request, currentUser));
    }

    @PatchMapping("/users/{id}/toggle-active")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<UserResponse> toggleActive(@PathVariable Long id) {
        return ResponseEntity.ok(authService.toggleActive(id));
    }

    @PatchMapping("/users/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<UserResponse> approve(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(authService.approveUser(id, currentUser));
    }

    @PatchMapping("/users/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<UserResponse> reject(@PathVariable Long id) {
        return ResponseEntity.ok(authService.rejectUser(id));
    }

    @PatchMapping("/users/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateRole(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal User currentUser) {
        String roleName = (String) body.get("roleName");
        if (roleName == null || roleName.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(authService.updateRole(id, roleName, currentUser));
    }
}
