package com.inventra.supplier;

import com.inventra.product.Product;
import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;

@Entity
@Table(name = "supplier_warehouse_stock",
       uniqueConstraints = @UniqueConstraint(name = "uq_supplier_product", columnNames = {"supplier_id", "product_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierWarehouseStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "available_quantity", nullable = false)
    private Integer availableQuantity = 0;

    @Column(name = "cost_price")
    private BigDecimal costPrice;

    @Column(name = "supplier_sku")
    private String supplierSku;  // Supplier's own product code
}
