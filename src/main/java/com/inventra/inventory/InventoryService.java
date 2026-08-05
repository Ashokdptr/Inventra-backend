package com.inventra.inventory;

import com.inventra.alert.AlertService;
import com.inventra.auth.User;
import com.inventra.common.exception.ResourceNotFoundException;
import com.inventra.product.Product;
import com.inventra.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class InventoryService {

    private final InventoryRepository     inventoryRepository;
    private final StockMovementRepository movementRepository;
    private final ProductRepository       productRepository;
    private final AlertService            alertService;

    @Transactional(readOnly = true)
    public List<InventoryResponse> getAll() {
        return inventoryRepository.findAll().stream().map(InventoryResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public InventoryResponse getByProductId(Long productId) {
        return inventoryRepository.findByProductId(productId)
            .map(InventoryResponse::from)
            .orElseThrow(() -> new ResourceNotFoundException("Inventory for product", productId));
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> getLowStock() {
        return inventoryRepository.findLowStock().stream().map(InventoryResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> getOutOfStock() {
        return inventoryRepository.findOutOfStock().stream().map(InventoryResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<StockMovementResponse> getAllMovements() {
        return movementRepository.findAllWithDetails().stream()
            .map(StockMovementResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<StockMovementResponse> getMovementsByProduct(Long productId) {
        return movementRepository.findByProductId(productId).stream()
            .map(StockMovementResponse::from).toList();
    }

    public InventoryResponse adjustStock(StockAdjustRequest req, User currentUser) {
        Product product = productRepository.findById(req.productId())
            .orElseThrow(() -> new ResourceNotFoundException("Product", req.productId()));

        Inventory inv = inventoryRepository.findByProductId(req.productId())
            .orElseGet(() -> Inventory.builder().product(product).currentStock(0).build());

        int newStock;
        if (req.movementType() == StockMovement.MovementType.IN) {
            newStock = inv.getCurrentStock() + req.quantity();
        } else {
            if (inv.getCurrentStock() < req.quantity())
                throw new IllegalArgumentException(
                    "Insufficient stock. Available: " + inv.getCurrentStock());
            newStock = inv.getCurrentStock() - req.quantity();
        }
        inv.setCurrentStock(newStock);
        inventoryRepository.save(inv);

        movementRepository.save(StockMovement.builder()
            .product(product).movementType(req.movementType())
            .quantity(req.quantity()).referenceType(req.referenceType())
            .notes(req.notes()).createdBy(currentUser).build());

        alertService.checkAndCreateAlert(product, newStock);
        return InventoryResponse.from(inv);
    }
}
