package com.inventra.supplier;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record WarehouseStockRequest(
    @NotNull Long productId,
    @NotNull @Min(0) Integer availableQuantity,
    BigDecimal costPrice,
    String supplierSku
) {}
