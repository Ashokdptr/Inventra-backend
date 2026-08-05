package com.inventra.auth;

public record AuthResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    Long userId,
    String name,
    String email,
    String phone,
    String department,
    String assignedCategories,
    String lastLoginAt,
    String role
) {
    public static AuthResponse of(String access, String refresh, User user) {
        return new AuthResponse(
            access, refresh, "Bearer",
            user.getId(), user.getName(),
            user.getEmail(), user.getPhone(), user.getDepartment(), user.getAssignedCategories(),
            user.getLastLoginAt() != null ? user.getLastLoginAt().toString() : null,
            user.getRole().getRoleName()
        );
    }
}
