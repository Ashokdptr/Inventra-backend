package com.inventra.inventory;

public record InventoryResponse(
    Long inventoryId, Long productId, String productName, String sku,
    Integer currentStock, Integer reorderLevel, String stockStatus, String lastUpdated
) {
    public static InventoryResponse from(Inventory inv) {
        int stock   = inv.getCurrentStock();
        int reorder = inv.getProduct().getReorderLevel();
        String status = stock == 0 ? "OUT_OF_STOCK" : stock <= reorder ? "LOW_STOCK" : "IN_STOCK";
        return new InventoryResponse(
            inv.getId(), inv.getProduct().getId(), inv.getProduct().getName(),
            inv.getProduct().getSku(), stock, reorder, status,
            inv.getLastUpdated() != null ? inv.getLastUpdated().toString() : null
        );
    }
}
