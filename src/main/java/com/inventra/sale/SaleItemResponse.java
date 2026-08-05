package com.inventra.sale;

import java.math.BigDecimal;

public record SaleItemResponse(
    Long id, Long productId, String productName,
    String categoryName,
    Integer quantity, BigDecimal unitPrice, BigDecimal subtotal
) {
    public static SaleItemResponse from(SaleItem i) {
        String catName = null;
        if (i.getProduct().getCategory() != null) {
            catName = i.getProduct().getCategory().getName();
        }
        return new SaleItemResponse(
            i.getId(),
            i.getProduct().getId(),
            i.getProduct().getName(),
            catName,
            i.getQuantity(),
            i.getUnitPrice(),
            i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity()))
        );
    }
}
