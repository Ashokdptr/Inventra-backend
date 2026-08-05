package com.inventra.purchaseorder;

import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(@NotNull PurchaseOrder.Status status) {}
