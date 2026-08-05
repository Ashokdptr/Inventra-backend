package com.inventra.purchaseorder;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record PurchaseOrderRequest(
    @NotNull Long supplierId,
    @NotNull String orderDate,
    String expectedDate,
    String notes,
    PurchaseOrder.PaymentStatus paymentStatus,
    @NotEmpty List<PoItemRequest> items
) {}
