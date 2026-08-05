package com.inventra.alert;

public record AlertResponse(
    Long id, Long productId, String productName,
    String alertType, String message, Boolean isRead, String createdAt
) {
    public static AlertResponse from(Alert a) {
        return new AlertResponse(
            a.getId(),
            a.getProduct() != null ? a.getProduct().getId()   : null,
            a.getProduct() != null ? a.getProduct().getName() : null,
            a.getAlertType().name(), a.getMessage(), a.getIsRead(),
            a.getCreatedAt() != null ? a.getCreatedAt().toString() : null
        );
    }
}
