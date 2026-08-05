package com.inventra.supplier;

public record SupplierResponse(
    Long id, String name, String email, String phone,
    String address, String contactName, String createdAt
) {
    public static SupplierResponse from(Supplier s) {
        return new SupplierResponse(
            s.getId(), s.getName(), s.getEmail(), s.getPhone(),
            s.getAddress(), s.getContactName(),
            s.getCreatedAt() != null ? s.getCreatedAt().toString() : null
        );
    }
}
