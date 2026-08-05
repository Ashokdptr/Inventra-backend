package com.inventra.supplier;

import java.math.BigDecimal;

public record SupplierWarehouseStockResponse(
    Long id,
    Long supplierId,
    String supplierName,
    Long productId,
    String productName,
    String sku,
    String supplierSku,
    Integer availableQuantity,
    BigDecimal costPrice
) {
    public static SupplierWarehouseStockResponse from(SupplierWarehouseStock stock) {
        String warhouseSku = stock.getSupplierSku() != null
            ? stock.getSupplierSku()
            : "WH-" + stock.getSupplier().getId() + "-" + stock.getProduct().getId();
        return new SupplierWarehouseStockResponse(
            stock.getId(),
            stock.getSupplier().getId(),
            stock.getSupplier().getName(),
            stock.getProduct().getId(),
            stock.getProduct().getName(),
            stock.getProduct().getSku(),
            warhouseSku,
            stock.getAvailableQuantity(),
            stock.getCostPrice()
        );
    }
}
