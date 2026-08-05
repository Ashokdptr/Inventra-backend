package com.inventra.purchaseorder;

import com.inventra.audit.AuditService;

import com.inventra.auth.User;
import com.inventra.auth.UserRepository;
import com.inventra.common.exception.ResourceNotFoundException;
import com.inventra.inventory.Inventory;
import com.inventra.inventory.InventoryRepository;
import com.inventra.inventory.StockMovement;
import com.inventra.inventory.StockMovementRepository;
import com.inventra.notification.NotificationService;
import com.inventra.product.Product;
import com.inventra.product.ProductRepository;
import com.inventra.supplier.Supplier;
import com.inventra.supplier.SupplierRepository;
import com.inventra.supplier.SupplierWarehouseStock;
import com.inventra.supplier.SupplierWarehouseStockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PurchaseOrderService {

    private final PurchaseOrderRepository          poRepository;
    private final SupplierRepository               supplierRepository;
    private final ProductRepository                productRepository;
    private final InventoryRepository              inventoryRepository;
    private final StockMovementRepository          movementRepository;
    private final SupplierWarehouseStockRepository warehouseStockRepository;
    private final NotificationService              notificationService;
    private final UserRepository                   userRepository;
    private final AuditService                     auditService;

    // ── Read ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PurchaseOrderResponse> getAll(User currentUser) {
        String role = currentUser.getRole().getRoleName();
        return poRepository.findAllWithDetails().stream()
                .filter(po -> !"SUPPLIER".equals(role)
                        || (po.getSupplier().getEmail() != null
                        && po.getSupplier().getEmail().equalsIgnoreCase(currentUser.getEmail())))
                .map(PurchaseOrderResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PurchaseOrderResponse getById(Long id) {
        return poRepository.findByIdWithDetails(id)
                .map(PurchaseOrderResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", id));
    }

    // ── Create ───────────────────────────────────────────────────────────────

    public PurchaseOrderResponse create(PurchaseOrderRequest req, User createdBy) {
        if (req.supplierId() == null) {
            throw new IllegalArgumentException("Supplier is required.");
        }
        if (req.orderDate() == null || req.orderDate().isBlank()) {
            throw new IllegalArgumentException("Order date is required.");
        }
        if (req.items() == null || req.items().isEmpty()) {
            throw new IllegalArgumentException("At least one item is required.");
        }

        Supplier supplier = supplierRepository.findById(req.supplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", req.supplierId()));

        LocalDate orderDate;
        try {
            orderDate = LocalDate.parse(req.orderDate(), DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid order date format. Expected yyyy-MM-dd, got: " + req.orderDate());
        }


        LocalDate expectedDate = null;
        if (req.expectedDate() != null && !req.expectedDate().isBlank()) {
            try {
                expectedDate = LocalDate.parse(req.expectedDate());
                if (expectedDate.isBefore(orderDate)) {
                    throw new IllegalArgumentException("Expected date cannot be before order date.");
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid expected date format.");
            }
        }

        PurchaseOrder po = PurchaseOrder.builder()
                .supplier(supplier)
                .orderDate(orderDate)
                .expectedDate(expectedDate)
                .status(PurchaseOrder.Status.PENDING)
                .paymentStatus(PurchaseOrder.PaymentStatus.PENDING)
                .notes(req.notes())
                .createdBy(createdBy)
                .items(new ArrayList<>())
                .build();

        BigDecimal total = BigDecimal.ZERO;
        for (PoItemRequest itemReq : req.items()) {
            if (itemReq.productId() == null) {
                throw new IllegalArgumentException("Product is required for each item.");
            }
            if (itemReq.quantity() == null || itemReq.quantity() < 1) {
                throw new IllegalArgumentException("Quantity must be at least 1.");
            }
            if (itemReq.unitPrice() == null || itemReq.unitPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Unit price must be 0 or greater.");
            }

            Product product = productRepository.findById(itemReq.productId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", itemReq.productId()));

            SupplierWarehouseStock stock = warehouseStockRepository
                    .findBySupplierIdAndProductId(supplier.getId(), product.getId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            supplier.getName() + " does not have '" + product.getName() + "' in warehouse."));

            if (stock.getAvailableQuantity() < itemReq.quantity()) {
                throw new IllegalArgumentException(
                        "Insufficient stock: " + product.getName() + " has only "
                                + stock.getAvailableQuantity() + " units available.");
            }


            PurchaseOrderItem item = PurchaseOrderItem.builder()
                    .purchaseOrder(po)
                    .product(product)
                    .quantity(itemReq.quantity())
                    .unitPrice(itemReq.unitPrice())
                    .build();

            item.setPurchaseOrder(po); // ✅ ensure relationship
            po.getItems().add(item);

            total = total.add(itemReq.unitPrice().multiply(BigDecimal.valueOf(itemReq.quantity())));
        }
        po.setTotalAmount(total);
        PurchaseOrder saved = poRepository.save(po);

        notifySupplierNewPO(saved, createdBy);
        auditService.log(AuditService.MOD_PO, "CREATED",
                "Purchase Order #" + saved.getId() + " created for supplier: " + saved.getSupplier().getName(),
                createdBy.getEmail(), role(createdBy), saved.getId(),
                "Total: ₹" + String.format("%.2f", saved.getTotalAmount()), AuditService.SEV_SUCCESS);
        return PurchaseOrderResponse.from(saved);
    }

    // ── Status Updates ───────────────────────────────────────────────────────

    public PurchaseOrderResponse updateStatus(Long id, UpdateStatusRequest req, User currentUser) {
        if (req.status() == null) {
            throw new IllegalArgumentException("Status is required.");
        }
        PurchaseOrder po = poRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", id));

        if (req.status() == PurchaseOrder.Status.COMPLETED) {
            if (po.getStatus() != PurchaseOrder.Status.SHIPPED
                    && po.getStatus() != PurchaseOrder.Status.APPROVED) {
                throw new IllegalArgumentException("Purchase order must be shipped/approved by supplier before completion.");
            }

            // ✅ Inventory is ONLY updated when payment has been confirmed (PAID).
            // For PENDING / PROCESSING / FAILED payment statuses, the order can still
            // be marked COMPLETED (goods received) but the stock will NOT be added to
            // inventory until the payment status is updated to PAID.
            if (po.getPaymentStatus() == PurchaseOrder.PaymentStatus.PAID) {
                receiveIntoInventory(po, currentUser);
                po.setStockReceived(true);

                notifySupplierUser(po.getSupplier().getEmail(),
                        "Purchase Order #" + po.getId() + " has been completed. Stock transferred to inventory.");
            } else {
                notifySupplierUser(po.getSupplier().getEmail(),
                        "Purchase Order #" + po.getId() + " has been marked as completed by the buyer. "
                                + "Stock will be transferred to inventory once payment is confirmed (PAID).");
            }
        }

        if (req.status() == PurchaseOrder.Status.CANCELLED
                && po.getStatus() == PurchaseOrder.Status.COMPLETED) {
            throw new IllegalArgumentException("A completed order cannot be cancelled.");
        }

        po.setStatus(req.status());
        PurchaseOrder saved = poRepository.save(po);

        // Audit status changes
        if (req.status() == PurchaseOrder.Status.COMPLETED) {
            String detail = saved.isStockReceived()
                    ? "PO #" + saved.getId() + " completed — stock transferred to inventory"
                    : "PO #" + saved.getId() + " completed — stock transfer PENDING (payment status: " + saved.getPaymentStatus() + ")";
            auditService.log(AuditService.MOD_PO, "COMPLETED",
                    detail,
                    currentUser.getEmail(), role(currentUser), saved.getId(),
                    "Supplier: " + saved.getSupplier().getName() + " | Total: ₹" + saved.getTotalAmount(), AuditService.SEV_SUCCESS);
        } else if (req.status() == PurchaseOrder.Status.CANCELLED) {
            auditService.log(AuditService.MOD_PO, "CANCELLED",
                    "PO #" + saved.getId() + " cancelled by " + currentUser.getEmail(),
                    currentUser.getEmail(), role(currentUser), saved.getId(), null, AuditService.SEV_WARN);
        }

        return PurchaseOrderResponse.from(saved);
    }

    public PurchaseOrderResponse updatePaymentStatus(Long id, PurchaseOrder.PaymentStatus status, User currentUser) {
        PurchaseOrder po = poRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", id));
        po.setPaymentStatus(status);

        // ✅ If the order was already marked COMPLETED (goods received) but stock
        // wasn't pushed to inventory yet because payment wasn't confirmed,
        // do it now that payment status has become PAID.
        if (status == PurchaseOrder.PaymentStatus.PAID
                && po.getStatus() == PurchaseOrder.Status.COMPLETED
                && !po.isStockReceived()) {
            receiveIntoInventory(po, currentUser);
            po.setStockReceived(true);
            notifySupplierUser(po.getSupplier().getEmail(),
                    "Payment for Purchase Order #" + po.getId() + " has been confirmed. Stock transferred to inventory.");
        }

        // PENDING / PROCESSING / FAILED payment statuses never add stock to inventory.
        // If stock was somehow already received and payment later fails/changes,
        // we do NOT reverse it automatically here — that requires a manual adjustment.

        PurchaseOrder savedPo = poRepository.save(po);
        auditService.log(AuditService.MOD_PAYMENT, "PO_PAYMENT_UPDATED",
                "PO #" + savedPo.getId() + " payment status → " + status + " (Supplier: " + savedPo.getSupplier().getName() + ")"
                        + (savedPo.isStockReceived() ? " — stock transferred to inventory" : ""),
                currentUser.getEmail(), role(currentUser), savedPo.getId(),
                "₹" + String.format("%.2f", savedPo.getTotalAmount()) + " — " + status, AuditService.SEV_INFO);
        return PurchaseOrderResponse.from(savedPo);
    }

    @Transactional(readOnly = true)
    public List<PurchaseOrderResponse> getByStatus(PurchaseOrder.Status status) {
        return poRepository.findByStatus(status).stream()
                .map(PurchaseOrderResponse::from)
                .toList();
    }

    // ── Supplier Actions ─────────────────────────────────────────────────────

    public PurchaseOrderResponse supplierDecision(Long id, PurchaseOrder.Status decision, User supplier) {
        if (decision != PurchaseOrder.Status.APPROVED && decision != PurchaseOrder.Status.REJECTED) {
            throw new IllegalArgumentException("Supplier can only APPROVE or REJECT.");
        }
        PurchaseOrder po = poRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", id));

        assertSupplierOwns(po, supplier);

        if (po.getStatus() != PurchaseOrder.Status.PENDING) {
            throw new IllegalArgumentException("Only PENDING orders can be actioned.");
        }
        if (decision == PurchaseOrder.Status.APPROVED) {
            validateWarehouseStock(po);
        }
        // When supplier approves, mark as SHIPPED (goods dispatched from supplier warehouse)
        PurchaseOrder.Status actualStatus = (decision == PurchaseOrder.Status.APPROVED)
                ? PurchaseOrder.Status.SHIPPED : decision;
        po.setStatus(actualStatus);
        PurchaseOrder saved = poRepository.save(po);

        if (po.getCreatedBy() != null) {
            String verb = actualStatus == PurchaseOrder.Status.SHIPPED ? "SHIPPED" : "REJECTED";
            notificationService.createInApp(po.getCreatedBy().getId(),
                    "🚛 Supplier " + supplier.getName() + " has " + verb
                            + " Purchase Order #" + po.getId() + "."
                            + (actualStatus == PurchaseOrder.Status.SHIPPED
                            ? " Order is on the way. Confirm receipt to update inventory." : ""));
        }
        auditService.log(AuditService.MOD_PO, actualStatus.name(),
                "PO #" + po.getId() + " " + (actualStatus == PurchaseOrder.Status.SHIPPED ? "shipped" : "rejected") + " by supplier " + supplier.getName(),
                supplier.getEmail(), "SUPPLIER", po.getId(),
                actualStatus == PurchaseOrder.Status.SHIPPED ? "Order dispatched" : "Rejected by supplier", AuditService.SEV_INFO);
        return PurchaseOrderResponse.from(saved);
    }

    public PurchaseOrderResponse supplierModify(Long id, ModifyPurchaseOrderRequest req, User supplier) {
        if (req.items() == null || req.items().isEmpty()) {
            throw new IllegalArgumentException("At least one item modification is required.");
        }
        PurchaseOrder po = poRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", id));

        assertSupplierOwns(po, supplier);
        if (po.getStatus() != PurchaseOrder.Status.PENDING) {
            throw new IllegalArgumentException("Only PENDING orders can be modified.");
        }

        for (ModifyPoItemRequest itemReq : req.items()) {
            if (itemReq.quantity() < 1) {
                throw new IllegalArgumentException("Modified quantity must be at least 1.");
            }
            PurchaseOrderItem item = po.getItems().stream()
                    .filter(i -> i.getId().equals(itemReq.itemId()))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrderItem", itemReq.itemId()));

            SupplierWarehouseStock stock = warehouseStockRepository
                    .findBySupplierIdAndProductId(po.getSupplier().getId(), item.getProduct().getId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "No warehouse stock for " + item.getProduct().getName()));

            if (stock.getAvailableQuantity() < itemReq.quantity()) {
                throw new IllegalArgumentException(
                        "Warehouse has only " + stock.getAvailableQuantity()
                                + " units of " + item.getProduct().getName());
            }
            item.setQuantity(itemReq.quantity());
            item.setPurchaseOrder(po); // ✅ ensure relationship remains intact
        }

        String note = req.notes() != null ? req.notes() : "";
        po.setNotes((po.getNotes() != null ? po.getNotes() + "\n" : "") + "Supplier modified: " + note);

        // ✅ recalculate total safely
        po.setTotalAmount(po.getItems().stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        return PurchaseOrderResponse.from(poRepository.save(po));
    }

    // ── Private Helpers ──────────────────────────────────────────────────────

    private void receiveIntoInventory(PurchaseOrder po, User currentUser) {
        for (PurchaseOrderItem item : po.getItems()) {
            SupplierWarehouseStock stock = warehouseStockRepository
                    .findBySupplierIdAndProductId(po.getSupplier().getId(), item.getProduct().getId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Warehouse stock not found for " + item.getProduct().getName()));

            if (stock.getAvailableQuantity() < item.getQuantity()) {
                throw new IllegalArgumentException(
                        "Insufficient warehouse stock for " + item.getProduct().getName());
            }

            stock.setAvailableQuantity(stock.getAvailableQuantity() - item.getQuantity());
            warehouseStockRepository.save(stock);

            Inventory inv = inventoryRepository.findByProductId(item.getProduct().getId())
                    .orElseGet(() -> Inventory.builder()
                            .product(item.getProduct()).currentStock(0).build());
            inv.setCurrentStock(inv.getCurrentStock() + item.getQuantity());
            inventoryRepository.save(inv);

            movementRepository.save(StockMovement.builder()
                    .product(item.getProduct())
                    .movementType(StockMovement.MovementType.IN)
                    .quantity(item.getQuantity())
                    .referenceType(StockMovement.ReferenceType.PURCHASE)
                    .referenceId(po.getId())
                    .createdBy(currentUser)
                    .build());
        }
    }

    private void notifySupplierNewPO(PurchaseOrder po, User createdBy) {
        if (po.getSupplier().getEmail() == null) return;
        userRepository.findByEmail(po.getSupplier().getEmail()).ifPresent(supplierUser ->
                notificationService.createInApp(supplierUser.getId(),
                        "New Purchase Order #" + po.getId() + " from " + createdBy.getName()
                                + ". Total: ₹" + po.getTotalAmount() + ". Please review and approve.")
        );
    }

    private void notifySupplierUser(String email, String message) {
        if (email == null) return;
        userRepository.findByEmail(email).ifPresent(u ->
                notificationService.createInApp(u.getId(), message));
    }

    private void assertSupplierOwns(PurchaseOrder po, User supplierUser) {
        if (po.getSupplier().getEmail() == null
                || !po.getSupplier().getEmail().equalsIgnoreCase(supplierUser.getEmail())) {
            throw new IllegalArgumentException("This order is not assigned to your supplier account.");
        }
    }

    private void validateWarehouseStock(PurchaseOrder po) {
        for (PurchaseOrderItem item : po.getItems()) {
            SupplierWarehouseStock stock = warehouseStockRepository
                    .findBySupplierIdAndProductId(po.getSupplier().getId(), item.getProduct().getId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "No warehouse stock for " + item.getProduct().getName() + ". Cannot approve."));
            if (stock.getAvailableQuantity() < item.getQuantity()) {
                throw new IllegalArgumentException(
                        "Cannot approve: only " + stock.getAvailableQuantity()
                                + " units available for " + item.getProduct().getName());
            }
        }
    }

    private String role(com.inventra.auth.User u) {
        if (u.getRole() == null) return "UNKNOWN";
        return u.getRole().getRoleName() != null ? u.getRole().getRoleName() : "UNKNOWN";
    }
}

