package com.inventra.auth;

public record UserResponse(
    Long id,
    String name,
    String email,
    String phone,
    String address,
    String role,
    String department,
    String assignedCategories,
    String approvalStatus,
    String approvedBy,
    Boolean isActive,
    String lastLoginAt,
    String createdAt
) {
    public static UserResponse from(User u) {
        return new UserResponse(
            u.getId(),
            u.getName(),
            u.getEmail(),
            u.getPhone(),
            u.getAddress(),
            u.getRole().getRoleName(),
            u.getDepartment(),
            u.getAssignedCategories(),
            u.getApprovalStatus() != null ? u.getApprovalStatus().name() : "APPROVED",
            u.getApprovedBy(),
            u.getIsActive(),
            u.getLastLoginAt()  != null ? u.getLastLoginAt().toString()  : null,
            u.getCreatedAt()    != null ? u.getCreatedAt().toString()    : null
        );
    }
}
