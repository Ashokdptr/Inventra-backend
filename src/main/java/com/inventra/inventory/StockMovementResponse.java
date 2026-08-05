package com.inventra.inventory;

public record StockMovementResponse(
    Long id, Long productId, String productName,
    String movementType, Integer quantity, String referenceType,
    String notes, String createdBy, String createdAt
) {
    public static StockMovementResponse from(StockMovement m) {
        return new StockMovementResponse(
            m.getId(), m.getProduct().getId(), m.getProduct().getName(),
            m.getMovementType().name(), m.getQuantity(), m.getReferenceType().name(),
            m.getNotes(),
            m.getCreatedBy() != null ? m.getCreatedBy().getName() : null,
            m.getCreatedAt() != null ? m.getCreatedAt().toString() : null
        );
    }
}
