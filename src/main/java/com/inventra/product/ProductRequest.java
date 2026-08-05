package com.inventra.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProductRequest(
    @NotBlank @Size(max = 200) String name,
    @Size(max = 100) String sku,
    @Size(max = 80) String barcode,
    String description,
    String imageUrl,
    LocalDate expiryDate,
    Long categoryId,
    Long supplierId,
    @NotNull @DecimalMin("0.0") BigDecimal price,
    @NotNull @DecimalMin("0.0") BigDecimal costPrice,
    @NotNull @Min(0) Integer reorderLevel,
    @Min(0) Integer currentStock
) {}
