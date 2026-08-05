package com.inventra.notification;

public record NotificationResponse(
    Long id, String type, String message,
    String status, Boolean isRead, String createdAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
            n.getId(), n.getType().name(), n.getMessage(),
            n.getStatus().name(), n.getIsRead(),
            n.getCreatedAt() != null ? n.getCreatedAt().toString() : null
        );
    }
}
