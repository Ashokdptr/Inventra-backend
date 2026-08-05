package com.inventra.sale;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SaleRequest(
    @Size(max = 200) String customerName,
    @Pattern(regexp = "^$|^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "must be a valid email address")
    @Size(max = 150) String customerEmail,
    @NotNull String saleDate,
    Sale.PaymentStatus paymentStatus,
    @NotEmpty List<SaleItemRequest> items
) {}
