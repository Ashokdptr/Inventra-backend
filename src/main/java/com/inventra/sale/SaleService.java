package com.inventra.sale;

import com.inventra.audit.AuditService;
import com.inventra.auth.User;
import com.inventra.common.exception.ResourceNotFoundException;
import com.inventra.inventory.Inventory;
import com.inventra.inventory.InventoryRepository;
import com.inventra.inventory.StockMovement;
import com.inventra.inventory.StockMovementRepository;
import com.inventra.product.Product;
import com.inventra.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SaleService {

    private final SaleRepository          saleRepository;
    private final ProductRepository       productRepository;
    private final InventoryRepository     inventoryRepository;
    private final StockMovementRepository movementRepository;
    private final AuditService            auditService;

    @Transactional(readOnly = true)
    public List<SaleResponse> getAll() {
        return saleRepository.findAllWithDetails().stream()
                .map(SaleResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public SaleResponse getById(Long id) {
        return saleRepository.findByIdWithDetails(id)
                .map(SaleResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Sale", id));
    }

    @Transactional(readOnly = true)
    public List<SaleResponse> getByDateRange(LocalDate from, LocalDate to) {
        return saleRepository.findByDateRange(from, to).stream()
                .map(SaleResponse::from).toList();
    }

    public SaleResponse create(SaleRequest req, User currentUser) {
        Sale sale = Sale.builder()
                .customerName(normalizeOptional(req.customerName()))
                .customerEmail(normalizeOptional(req.customerEmail()))
                .saleDate(LocalDate.parse(req.saleDate()))
                .paymentStatus(req.paymentStatus() != null ? req.paymentStatus() : Sale.PaymentStatus.PENDING)
                .createdBy(currentUser)
                .items(new ArrayList<>())
                .build();

        BigDecimal total = BigDecimal.ZERO;

        for (SaleItemRequest itemReq : req.items()) {
            Product product = productRepository.findById(itemReq.productId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", itemReq.productId()));

            Inventory inv = inventoryRepository.findByProductId(itemReq.productId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "No inventory record for product: " + product.getName()));

            if (inv.getCurrentStock() < itemReq.quantity()) {
                throw new IllegalArgumentException(
                        "Insufficient stock for '" + product.getName()
                        + "'. Available: " + inv.getCurrentStock());
            }

            inv.setCurrentStock(inv.getCurrentStock() - itemReq.quantity());
            inventoryRepository.save(inv);

            SaleItem si = SaleItem.builder()
                    .sale(sale)
                    .product(product)
                    .quantity(itemReq.quantity())
                    .unitPrice(itemReq.unitPrice())
                    .build();
            sale.getItems().add(si);
            total = total.add(itemReq.unitPrice().multiply(BigDecimal.valueOf(itemReq.quantity())));

            movementRepository.save(StockMovement.builder()
                    .product(product)
                    .movementType(StockMovement.MovementType.OUT)
                    .quantity(itemReq.quantity())
                    .referenceType(StockMovement.ReferenceType.SALE)
                    .createdBy(currentUser)
                    .build());
        }

        sale.setTotalAmount(total);
        Sale saved = saleRepository.save(sale);

        auditService.log(AuditService.MOD_SALE, "CREATED",
                "Sale #" + saved.getId() + " created by " + currentUser.getEmail()
                + " | Customer: " + (saved.getCustomerName() != null ? saved.getCustomerName() : "Walk-in"),
                currentUser.getEmail(), role(currentUser), saved.getId(),
                "Total: ₹" + saved.getTotalAmount().toPlainString(), AuditService.SEV_SUCCESS);

        return SaleResponse.from(saved);
    }

    public SaleResponse updatePaymentStatus(Long id, Sale.PaymentStatus status) {
        Sale sale = saleRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sale", id));
        sale.setPaymentStatus(status);
        Sale saved = saleRepository.save(sale);

        auditService.log(AuditService.MOD_PAYMENT, "SALE_PAYMENT_UPDATED",
                "Sale #" + saved.getId() + " payment status changed to " + status,
                "SYSTEM", "ADMIN", saved.getId(),
                status.name(), AuditService.SEV_INFO);

        return SaleResponse.from(saved);
    }

    public void delete(Long id, User currentUser) {
        Sale sale = saleRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sale", id));

        for (SaleItem item : sale.getItems()) {
            Product product = item.getProduct();
            Inventory inv = inventoryRepository.findByProductId(product.getId())
                    .orElseGet(() -> Inventory.builder().product(product).currentStock(0).build());

            inv.setCurrentStock(inv.getCurrentStock() + item.getQuantity());
            inventoryRepository.save(inv);

            movementRepository.save(StockMovement.builder()
                    .product(product)
                    .movementType(StockMovement.MovementType.IN)
                    .quantity(item.getQuantity())
                    .referenceType(StockMovement.ReferenceType.ADJUSTMENT)
                    .notes("Stock restored after removing sale #" + id)
                    .createdBy(currentUser)
                    .build());
        }

        Long saleId = sale.getId();
        saleRepository.delete(sale);

        auditService.log(AuditService.MOD_SALE, "DELETED",
                "Sale #" + saleId + " deleted by " + currentUser.getEmail(),
                currentUser.getEmail(), role(currentUser), saleId, null, AuditService.SEV_WARN);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String normalizeOptional(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String role(com.inventra.auth.User u) {
        if (u.getRole() == null) return "UNKNOWN";
        return u.getRole().getRoleName() != null ? u.getRole().getRoleName() : "UNKNOWN";
    }
}
