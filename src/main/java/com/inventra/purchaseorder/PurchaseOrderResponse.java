package com.inventra.purchaseorder;

import java.math.BigDecimal;
import java.util.List;

public record PurchaseOrderResponse(
    Long id, Long supplierId, String supplierName,
    String orderDate, String expectedDate, String status,
    BigDecimal totalAmount, String paymentStatus, boolean stockReceived, String notes,
    List<PoItemResponse> items, String createdBy, String createdAt
) {
    public static PurchaseOrderResponse from(PurchaseOrder po) {
        return new PurchaseOrderResponse(
            po.getId(),
            po.getSupplier().getId(),
            po.getSupplier().getName(),
            po.getOrderDate() != null ? po.getOrderDate().toString() : null,
            po.getExpectedDate() != null ? po.getExpectedDate().toString() : null,
            po.getStatus().name(),
            po.getTotalAmount(),
            po.getPaymentStatus() != null ? po.getPaymentStatus().name() : null,
            po.isStockReceived(),
            po.getNotes(),
            po.getItems() != null
                ? po.getItems().stream().map(PoItemResponse::from).toList()
                : List.of(),
            po.getCreatedBy() != null ? po.getCreatedBy().getName() : null,
            po.getCreatedAt() != null ? po.getCreatedAt().toString() : null
        );
    }
}
