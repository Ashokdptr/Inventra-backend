package com.inventra.supplier;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SupplierRequest(
    @NotBlank @Size(max = 200) String name,
    @Email @Pattern(regexp = "^$|^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "must be a valid email address") @Size(max = 150) String email,
    @Size(max = 30)            String phone,
                               String address,
    @Size(max = 100)           String contactName
) {}
