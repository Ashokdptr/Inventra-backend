package com.inventra.purchaseorder;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.inventra.auth.User;
import com.inventra.supplier.Supplier;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "purchase_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrder {

    public enum Status { PENDING, APPROVED, SHIPPED, REJECTED, COMPLETED, CANCELLED }
    public enum PaymentStatus { PENDING, PROCESSING, PAID, FAILED, APPROVED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ✅ Supplier mapping
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    // ✅ Order date
    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    // ✅ Expected date with proper JSON format
    @Column(name = "expected_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expectedDate;

    // ✅ Status
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;

    // ✅ Total amount with precision
    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    // ✅ Payment status
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    // ✅ True once items have actually been added to inventory.
    //    This only happens once the order is COMPLETED AND paymentStatus == PAID.
    @Column(name = "stock_received", nullable = false)
    @Builder.Default
    private boolean stockReceived = false;

    private String notes;

    // ✅ Created by user
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    // ✅ Timestamp
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ✅ Items mapping with cascade and orphan removal
    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PurchaseOrderItem> items = new ArrayList<>();
}
