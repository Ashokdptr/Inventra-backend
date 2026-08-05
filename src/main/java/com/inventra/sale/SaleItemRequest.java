package com.inventra.sale;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SaleItemRequest(
    @NotNull Long productId,
    @NotNull @Min(1) Integer quantity,
    @NotNull @DecimalMin("0.0") BigDecimal unitPrice
) {}
