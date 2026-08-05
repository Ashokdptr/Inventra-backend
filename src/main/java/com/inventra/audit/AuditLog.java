package com.inventra.audit;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_action",    columnList = "action"),
    @Index(name = "idx_audit_module",    columnList = "module"),
    @Index(name = "idx_audit_user",      columnList = "performed_by"),
    @Index(name = "idx_audit_created",   columnList = "created_at")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditLog {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** e.g. SALE, PURCHASE_ORDER, USER, PRODUCT, SUPPLIER, INVENTORY, AUTH, PAYMENT */
    @Column(nullable = false, length = 50)
    private String module;

    /** e.g. CREATED, UPDATED, DELETED, LOGIN, LOGOUT, APPROVED, SHIPPED, COMPLETED, PAYMENT_PAID */
    @Column(nullable = false, length = 60)
    private String action;

    /** Human-readable summary of what happened */
    @Column(nullable = false, length = 500)
    private String description;

    /** The user who triggered the event */
    @Column(name = "performed_by", length = 150)
    private String performedBy;

    /** Role of the user at the time */
    @Column(name = "user_role", length = 50)
    private String userRole;

    /** Optional: related entity ID (sale id, PO id, product id, etc.) */
    @Column(name = "entity_id")
    private Long entityId;

    /** Optional: e.g. "₹5,400.00", "Stock: 50→55", "PENDING→SHIPPED" */
    @Column(name = "extra_info", length = 300)
    private String extraInfo;

    /** Severity: INFO, WARNING, SUCCESS, DANGER */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String severity = "INFO";

    /** Client IP if available */
    @Column(name = "ip_address", length = 60)
    private String ipAddress;

    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
}
