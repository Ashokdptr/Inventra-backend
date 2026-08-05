package com.inventra.inventory;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record StockAdjustRequest(
    @NotNull Long productId,
    @NotNull @Min(1) Integer quantity,
    @NotNull StockMovement.MovementType movementType,
    @NotNull StockMovement.ReferenceType referenceType,
    String notes
) {}
