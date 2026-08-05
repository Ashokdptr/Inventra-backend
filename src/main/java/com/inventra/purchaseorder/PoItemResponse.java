package com.inventra.purchaseorder;

import java.math.BigDecimal;

public record PoItemResponse(
    Long id, Long productId, String productName,
    Integer quantity, BigDecimal unitPrice, BigDecimal subtotal
) {
    public static PoItemResponse from(PurchaseOrderItem i) {
        return new PoItemResponse(
            i.getId(), i.getProduct().getId(), i.getProduct().getName(),
            i.getQuantity(), i.getUnitPrice(),
            i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity()))
        );
    }
}
