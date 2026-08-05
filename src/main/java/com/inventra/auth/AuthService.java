package com.inventra.auth;

import com.inventra.audit.AuditService;
import com.inventra.common.email.EmailService;
import com.inventra.common.exception.ResourceNotFoundException;
import com.inventra.common.util.JwtUtil;
import com.inventra.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository        userRepository;
    private final RoleRepository        roleRepository;
    private final AuthenticationManager authManager;
    private final JwtUtil               jwtUtil;
    private final PasswordEncoder       passwordEncoder;
    private final OtpService            otpService;
    private final EmailService          emailService;
    private final AuditService          auditService;
    private final NotificationService   notificationService;

    // ── Login ────────────────────────────────────────────────────────────────
    public AuthResponse login(LoginRequest request) {
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        if (!user.getIsActive()) {
            throw new IllegalArgumentException("Your account is disabled. Contact an administrator.");
        }
        if (user.getApprovalStatus() == User.ApprovalStatus.PENDING) {
            throw new IllegalArgumentException("Your account is pending approval.");
        }
        if (user.getApprovalStatus() == User.ApprovalStatus.REJECTED) {
            throw new IllegalArgumentException("Your account registration was rejected.");
        }

        user.setLastLoginAt(java.time.LocalDateTime.now());
        userRepository.save(user);

        auditService.log(AuditService.MOD_AUTH, "LOGIN",
                "User logged in: " + user.getEmail(),
                user.getEmail(), user.getRole().getRoleName(), user.getId(), null, AuditService.SEV_INFO);

        String accessToken = jwtUtil.generateToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);
        return AuthResponse.of(accessToken, refreshToken, user);
    }

    // ── Register (public self-registration — STAFF or SUPPLIER only) ─────────
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }
        if (request.phone() != null && !request.phone().isBlank()
                && userRepository.existsByPhone(request.phone().trim())) {
            throw new IllegalArgumentException("An account with this phone number already exists.");
        }

        String normalized = request.role() != null ? request.role().trim().toUpperCase() : "STAFF";
        if (!List.of("STAFF", "SUPPLIER").contains(normalized)) {
            throw new IllegalArgumentException("Self-registration is only allowed for STAFF or SUPPLIER roles.");
        }

        Role role = roleRepository.findByRoleName(normalized)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + normalized));

        User user = User.builder()
                .name(request.name().trim())
                .email(request.email().trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.password()))
                .phone(request.phone())
                .department(request.department())
                .role(role)
                .isActive(false)
                .approvalStatus(User.ApprovalStatus.PENDING)
                .build();

        User saved = userRepository.save(user);

        if ("SUPPLIER".equals(normalized)) {
            ensureSupplierProfile(saved.getName(), saved.getEmail(), saved.getPhone(), saved.getDepartment());
        }

        // ✅ Notify all Admins and Managers about the new registration awaiting approval
        notifyAdminsAndManagers(
                "New " + normalized + " registration: " + saved.getName() + " (" + saved.getEmail() + ") "
                        + "is awaiting approval in User Management."
        );

        auditService.log(AuditService.MOD_AUTH, "REGISTER",
                "New " + normalized + " account registered: " + saved.getEmail() + " — pending approval",
                saved.getEmail(), normalized, saved.getId(), null, AuditService.SEV_INFO);

        // ✅ Pass both message and UserResponse to RegisterResponse
        return new RegisterResponse(
                "Registration submitted! An administrator will review your request.",
                UserResponse.from(saved)
        );
    }

    /** Sends an in-app notification to every ADMIN and MANAGER user. */
    private void notifyAdminsAndManagers(String message) {
        userRepository.findByRole_RoleNameIn(List.of("ADMIN", "MANAGER"))
                .forEach(u -> notificationService.createInApp(u.getId(), message));
    }


    public UserResponse createUser(CreateUserRequest request, User currentUser) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }
        if (request.phone() != null && !request.phone().isBlank()
                && userRepository.existsByPhone(request.phone().trim())) {
            throw new IllegalArgumentException("An account with this phone number already exists.");
        }

        Role role = roleRepository.findById(request.roleId())
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + request.roleId()));

        if ("ADMIN".equals(currentUser.getRole().getRoleName())) {
            if ("ADMIN".equals(role.getRoleName())) {
                throw new IllegalArgumentException("Admins cannot create another Admin account.");
            }
        } else if ("MANAGER".equals(currentUser.getRole().getRoleName())) {
            if (!"STAFF".equals(role.getRoleName())) {
                throw new IllegalArgumentException("Managers can only create Staff accounts.");
            }
        }

        User user = User.builder()
                .name(request.name().trim())
                .email(request.email().trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.password()))
                .phone(request.phone())
                .department(request.department())
                .assignedCategories(request.assignedCategories())
                .role(role)
                .isActive(true)
                .approvalStatus(User.ApprovalStatus.APPROVED)
                .build();

        User saved = userRepository.save(user);

        if ("SUPPLIER".equals(role.getRoleName())) {
            ensureSupplierProfile(saved.getName(), saved.getEmail(), saved.getPhone(), saved.getDepartment());
        }

        return UserResponse.from(saved);
    }


    // ── Approval ─────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream().map(UserResponse::from).toList();
    }

    public UserResponse toggleActive(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        user.setIsActive(!user.getIsActive());
        return UserResponse.from(userRepository.save(user));
    }

    public UserResponse approveUser(Long id, User currentUser) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        user.setApprovalStatus(User.ApprovalStatus.APPROVED);
        user.setIsActive(true);
        user.setApprovedBy(currentUser.getName());
        return UserResponse.from(userRepository.save(user));
    }

    public UserResponse rejectUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        user.setApprovalStatus(User.ApprovalStatus.REJECTED);
        user.setIsActive(false);
        return UserResponse.from(userRepository.save(user));
    }

    // ── Role / Password Management ───────────────────────────────────────────
    public UserResponse updateRole(Long userId, String roleName, User currentUser) {
        if (userId.equals(currentUser.getId())) {
            throw new IllegalArgumentException("You cannot change your own role.");
        }

        User target = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        String normalized = roleName.trim().toUpperCase();
        if (!List.of("ADMIN", "MANAGER", "STAFF", "SUPPLIER").contains(normalized)) {
            throw new IllegalArgumentException("Invalid role: " + roleName);
        }
        if ("ADMIN".equals(normalized)) {
            throw new IllegalArgumentException("Cannot assign ADMIN role through this endpoint.");
        }

        Role role = roleRepository.findByRoleName(normalized)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + normalized));

        target.setRole(role);
        return UserResponse.from(userRepository.save(target));
    }

    public UserResponse updateProfile(User currentUser, String name, String phone, String address, String department) {
        if (name != null && !name.isBlank()) currentUser.setName(name.trim());
        if (phone != null) {
            String trimmedPhone = phone.trim();
            if (!trimmedPhone.isEmpty() && !trimmedPhone.equals(currentUser.getPhone())) {
                userRepository.findByPhone(trimmedPhone).ifPresent(existing -> {
                    if (!existing.getId().equals(currentUser.getId())) {
                        throw new IllegalArgumentException("An account with this phone number already exists.");
                    }
                });
            }
            currentUser.setPhone(trimmedPhone);
        }
        if (address != null) currentUser.setAddress(address.trim());
        if (department != null) currentUser.setDepartment(department.trim());
        return UserResponse.from(userRepository.save(currentUser));
    }

    public Map<String, String> changePassword(User currentUser, String currentPassword, String newPassword) {
        if (!passwordEncoder.matches(currentPassword, currentUser.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }
        if (newPassword.length() < 6) {
            throw new IllegalArgumentException("New password must be at least 6 characters.");
        }
        currentUser.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(currentUser);
        return Map.of("message", "Password changed successfully.");
    }

    // ── OTP Login ────────────────────────────────────────────────────────────

    /** Step 1: request OTP — validates user exists and is active, then sends OTP email */
    public Map<String, String> requestOtp(String email) {
        User user = userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new IllegalArgumentException("No account found with this email."));
        if (!user.getIsActive()) {
            throw new IllegalArgumentException("Account is inactive. Contact your administrator.");
        }
        String otp = otpService.generateOtp(user.getEmail());
        emailService.sendOtp(user.getEmail(), otp, user.getName());
        return Map.of("message", "OTP sent to " + email + ". Check your inbox — it expires in 10 minutes.");
    }

    /** Step 2: verify OTP and return JWT */
    public AuthResponse verifyOtpLogin(String email, String otp) {
        User user = userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
        if (!user.getIsActive()) {
            throw new IllegalArgumentException("Account is inactive.");
        }
        if (!otpService.verifyOtp(user.getEmail(), otp)) {
            throw new IllegalArgumentException("Invalid or expired OTP. Please request a new one.");
        }
        auditService.log(AuditService.MOD_AUTH, "OTP_LOGIN",
                "User logged in via OTP: " + user.getEmail(),
                user.getEmail(), role(user), user.getId(), null, AuditService.SEV_INFO);
        user.setLastLoginAt(java.time.LocalDateTime.now());
        userRepository.save(user);
        String accessToken = jwtUtil.generateToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);
        return AuthResponse.of(accessToken, refreshToken, user);
    }

    // ── Forgot / Reset Password ──────────────────────────────────────────────

    public Map<String, String> forgotPassword(String email) {
        User user = userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new IllegalArgumentException("No account found with this email."));
        String token = otpService.generateResetToken(user.getEmail());
        emailService.sendPasswordResetLink(user.getEmail(), token, user.getName());
        return Map.of("message", "Password reset link sent to " + email + ". Check your inbox — it expires in 30 minutes.");
    }

    public Map<String, String> resetPassword(String token, String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters.");
        }
        String email = otpService.getEmailForResetToken(token);
        if (email == null) {
            throw new IllegalArgumentException("Reset link is invalid or has expired. Please request a new one.");
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        otpService.deleteResetToken(token);
        return Map.of("message", "Password reset successfully. You can now log in.");
    }

    public Map<String, String> deactivateOwnAccount(User currentUser) {
        currentUser.setIsActive(false);
        userRepository.save(currentUser);
        auditService.log(AuditService.MOD_USER, "ACCOUNT_DELETED",
                "Account deactivated by user: " + currentUser.getEmail(),
                currentUser.getEmail(), role(currentUser), currentUser.getId(), null, AuditService.SEV_WARN);
        return Map.of("message", "Account deactivated. Contact an administrator to reactivate.");
    }

    private String role(User u) {
        if (u.getRole() == null) return "UNKNOWN";
        return u.getRole().getRoleName() != null ? u.getRole().getRoleName() : "UNKNOWN";
    }

    // ── Internal helpers ─────────────────────────────────────────────────────
    private void ensureSupplierProfile(String name, String email, String phone, String department) {
        applicationContext.getBean(com.inventra.supplier.SupplierService.class)
                .ensureSupplierProfile(name, email, phone, department);
    }

    @org.springframework.context.annotation.Lazy
    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.context.ApplicationContext applicationContext;
}
