package com.inventra.audit;

import java.time.LocalDateTime;

public record AuditLogResponse(
    Long          id,
    String        module,
    String        action,
    String        description,
    String        performedBy,
    String        userRole,
    Long          entityId,
    String        extraInfo,
    String        severity,
    String        ipAddress,
    LocalDateTime createdAt
) {
    public static AuditLogResponse from(AuditLog a) {
        return new AuditLogResponse(
            a.getId(), a.getModule(), a.getAction(), a.getDescription(),
            a.getPerformedBy(), a.getUserRole(), a.getEntityId(),
            a.getExtraInfo(), a.getSeverity(), a.getIpAddress(), a.getCreatedAt()
        );
    }
}
