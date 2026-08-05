package com.inventra.product;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProductResponse(
    Long id, String name, String sku, String barcode, String description, String imageUrl, LocalDate expiryDate,
    Long categoryId, String categoryName, String parentCategoryName,
    Long supplierId, String supplierName,
    BigDecimal price, BigDecimal costPrice,
    Integer reorderLevel, Integer currentStock,
    String stockStatus, String createdAt
) {
    public static ProductResponse from(Product p, Integer stock) {
        String status = (stock == null || stock == 0) ? "OUT_OF_STOCK"
                      : (stock <= p.getReorderLevel())  ? "LOW_STOCK"
                      : "IN_STOCK";
        String catName    = p.getCategory() != null ? p.getCategory().getName()   : null;
        String parentName = (p.getCategory() != null && p.getCategory().getParent() != null)
                            ? p.getCategory().getParent().getName() : null;
        return new ProductResponse(
            p.getId(), p.getName(), p.getSku(), p.getBarcode(), p.getDescription(), p.getImageUrl(), p.getExpiryDate(),
            p.getCategory() != null ? p.getCategory().getId() : null,
            catName, parentName,
            p.getSupplier() != null ? p.getSupplier().getId()   : null,
            p.getSupplier() != null ? p.getSupplier().getName() : null,
            p.getPrice(), p.getCostPrice(), p.getReorderLevel(),
            stock, status,
            p.getCreatedAt() != null ? p.getCreatedAt().toString() : null
        );
    }
}
