package com.inventra.invoice;

import com.inventra.auth.User;
import com.inventra.sale.Sale;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name = "invoices")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Invoice {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false)
    private String invoiceNumber;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id")
    private Sale sale;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String customerAddress;
    private BigDecimal subtotal;
    private BigDecimal taxRate;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private String notes;
    @Enumerated(EnumType.STRING)
    private InvoiceStatus status;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;
    @CreationTimestamp
    private LocalDateTime createdAt;
    public enum InvoiceStatus { DRAFT, ISSUED, PAID, CANCELLED }
}
