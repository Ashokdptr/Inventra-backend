package com.inventra.purchaseorder;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ModifyPoItemRequest(
    @NotNull Long itemId,
    @NotNull @Min(1) Integer quantity
) {}
