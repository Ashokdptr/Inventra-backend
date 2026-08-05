package com.inventra.sale;

import java.math.BigDecimal;
import java.util.List;

public record SaleResponse(
    Long id, String customerName, String customerEmail,
    String saleDate, BigDecimal totalAmount, String paymentStatus,
    List<SaleItemResponse> items, String createdBy, String createdAt
) {
    public static SaleResponse from(Sale s) {
        return new SaleResponse(
            s.getId(), s.getCustomerName(), s.getCustomerEmail(),
            s.getSaleDate() != null ? s.getSaleDate().toString() : null,
            s.getTotalAmount(),
            s.getPaymentStatus() != null ? s.getPaymentStatus().name() : null,
            s.getItems() != null
                ? s.getItems().stream().map(SaleItemResponse::from).toList()
                : List.of(),
            s.getCreatedBy() != null ? s.getCreatedBy().getName() : null,
            s.getCreatedAt() != null ? s.getCreatedAt().toString() : null
        );
    }
}
