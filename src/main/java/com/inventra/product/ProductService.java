package com.inventra.product;

import com.inventra.audit.AuditService;

import com.inventra.category.Category;
import com.inventra.category.CategoryRepository;
import com.inventra.common.exception.ResourceNotFoundException;
import com.inventra.inventory.Inventory;
import com.inventra.inventory.InventoryRepository;
import com.inventra.inventory.StockMovementRepository;
import com.inventra.supplier.Supplier;
import com.inventra.supplier.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {

    private final ProductRepository   productRepository;
    private final AuditService         auditService;
    private final CategoryRepository  categoryRepository;
    private final SupplierRepository  supplierRepository;
    private final InventoryRepository inventoryRepository;
    private final StockMovementRepository movementRepository;

    @Transactional(readOnly = true)
    public List<ProductResponse> getAll() {
        return productRepository.findAllWithDetails().stream()
                .map(p -> {
                    final Integer stock = inventoryRepository.findByProductId(p.getId())
                            .map(Inventory::getCurrentStock).orElse(0);
                    return ProductResponse.from(p, stock);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> search(String keyword, Long categoryId, Long supplierId) {
        return productRepository.searchProducts(keyword, categoryId, supplierId).stream()
                .map(p -> {
                    final Integer stock = inventoryRepository.findByProductId(p.getId())
                            .map(Inventory::getCurrentStock).orElse(0);
                    return ProductResponse.from(p, stock);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        final Product p = findOrThrow(id);
        final Integer stock = inventoryRepository.findByProductId(id)
                .map(Inventory::getCurrentStock).orElse(0);
        return ProductResponse.from(p, stock);
    }

    public ProductResponse create(ProductRequest req) {
        final String resolvedSku = resolveSku(req.sku(), req.name());
        if (productRepository.existsBySku(resolvedSku)) {
            throw new IllegalArgumentException("SKU already exists: " + resolvedSku);
        }

        Product product = buildProduct(new Product(), req);
        product.setSku(resolvedSku);
        product = productRepository.save(product);

        final Integer stock = req.currentStock() == null ? 0 : req.currentStock();
        inventoryRepository.save(Inventory.builder().product(product).currentStock(stock).build());

        return ProductResponse.from(product, stock);
    }

    public ProductResponse update(Long id, ProductRequest req) {
        final Product existingProduct = findOrThrow(id);

        final String resolvedSku = resolveSku(req.sku(), req.name());
        if (!existingProduct.getSku().equals(resolvedSku) && productRepository.existsBySku(resolvedSku)) {
            throw new IllegalArgumentException("SKU already exists: " + resolvedSku);
        }

        Product updatedProduct = buildProduct(existingProduct, req);
        updatedProduct.setSku(resolvedSku);

        Inventory inventory = inventoryRepository.findByProductId(id)
                .orElseGet(() -> Inventory.builder().product(updatedProduct).currentStock(0).build());

        if (req.currentStock() != null) {
            inventory.setCurrentStock(req.currentStock());
            inventoryRepository.save(inventory);
        }

        final Integer stock = inventory.getCurrentStock();
        ProductResponse updated = ProductResponse.from(productRepository.save(updatedProduct), stock);
        auditService.log(AuditService.MOD_PRODUCT, "UPDATED",
                "Product updated: " + updated.name() + " | SKU: " + updated.sku(),
                "ADMIN/MANAGER", "ADMIN", updated.id(), null, AuditService.SEV_INFO);
        return updated;
    }

    public void delete(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product", id);
        }
        inventoryRepository.deleteByProductId(id);
        movementRepository.deleteByProductId(id);
        inventoryRepository.flush();
        productRepository.deleteById(id);
        auditService.log(AuditService.MOD_PRODUCT, "DELETED",
                "Product deleted: ID #" + id,
                "ADMIN/MANAGER", "ADMIN", id, null, AuditService.SEV_WARN);
    }

    private Product buildProduct(Product p, ProductRequest req) {
        p.setName(req.name());
        p.setBarcode(resolveBarcode(req.barcode(), req.sku(), req.name()));
        p.setDescription(req.description());
        p.setImageUrl(req.imageUrl());
        p.setExpiryDate(req.expiryDate());
        p.setPrice(req.price());
        p.setCostPrice(req.costPrice());
        p.setReorderLevel(req.reorderLevel());

        if (req.categoryId() != null) {
            Category cat = categoryRepository.findById(req.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", req.categoryId()));
            p.setCategory(cat);
        } else {
            p.setCategory(null);
        }

        if (req.supplierId() != null) {
            Supplier sup = supplierRepository.findById(req.supplierId())
                    .orElseThrow(() -> new ResourceNotFoundException("Supplier", req.supplierId()));
            p.setSupplier(sup);
        } else {
            p.setSupplier(null);
        }

        return p;
    }

    private String resolveSku(String sku, String name) {
        if (sku != null && !sku.isBlank()) return sku.trim().toUpperCase();
        String prefix = name == null ? "INV" : name.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        if (prefix.length() > 6) prefix = prefix.substring(0, 6);
        if (prefix.isBlank()) prefix = "INV";
        return prefix + "-" + System.currentTimeMillis();
    }

    private String resolveBarcode(String barcode, String sku, String name) {
        if (barcode != null && !barcode.isBlank()) return barcode.trim();
        return "INV-" + resolveSku(sku, name);
    }

    private Product findOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }
}
