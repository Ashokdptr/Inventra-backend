package com.inventra.sale;

import jakarta.validation.constraints.NotNull;

public record UpdatePaymentStatusRequest(@NotNull Sale.PaymentStatus paymentStatus) {}
