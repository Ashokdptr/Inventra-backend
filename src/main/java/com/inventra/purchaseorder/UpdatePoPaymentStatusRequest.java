package com.inventra.purchaseorder;

import jakarta.validation.constraints.NotNull;

public record UpdatePoPaymentStatusRequest(@NotNull PurchaseOrder.PaymentStatus paymentStatus) {}
