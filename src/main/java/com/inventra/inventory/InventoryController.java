package com.inventra.inventory;

import com.inventra.auth.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    public ResponseEntity<List<InventoryResponse>> getAll() {
        return ResponseEntity.ok(inventoryService.getAll());
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<InventoryResponse> getByProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(inventoryService.getByProductId(productId));
    }

    @GetMapping("/low-stock")
    public ResponseEntity<List<InventoryResponse>> getLowStock() {
        return ResponseEntity.ok(inventoryService.getLowStock());
    }

    @GetMapping("/out-of-stock")
    public ResponseEntity<List<InventoryResponse>> getOutOfStock() {
        return ResponseEntity.ok(inventoryService.getOutOfStock());
    }

    @GetMapping("/movements")
    public ResponseEntity<List<StockMovementResponse>> getAllMovements() {
        return ResponseEntity.ok(inventoryService.getAllMovements());
    }

    @GetMapping("/movements/product/{productId}")
    public ResponseEntity<List<StockMovementResponse>> getMovementsByProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(inventoryService.getMovementsByProduct(productId));
    }

    @PostMapping("/adjust")
    public ResponseEntity<InventoryResponse> adjustStock(
            @Valid @RequestBody StockAdjustRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(inventoryService.adjustStock(request, currentUser));
    }
}
