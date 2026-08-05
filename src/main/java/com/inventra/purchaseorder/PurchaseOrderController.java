package com.inventra.purchaseorder;

import com.inventra.auth.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/purchase-orders")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private static final Logger log = LoggerFactory.getLogger(PurchaseOrderController.class);

    private final PurchaseOrderService poService;

    @GetMapping
    public ResponseEntity<List<PurchaseOrderResponse>> getAll(@AuthenticationPrincipal User currentUser) {
        log.info("Fetching all purchase orders for user: {}", currentUser.getEmail());
        return ResponseEntity.ok(poService.getAll(currentUser));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PurchaseOrderResponse> getById(@PathVariable Long id) {
        log.info("Fetching purchase order by ID: {}", id);
        return ResponseEntity.ok(poService.getById(id));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<PurchaseOrderResponse>> getByStatus(@PathVariable PurchaseOrder.Status status) {
        log.info("Fetching purchase orders by status: {}", status);
        return ResponseEntity.ok(poService.getByStatus(status));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<PurchaseOrderResponse> create(
            @Valid @RequestBody PurchaseOrderRequest request,
            @AuthenticationPrincipal User currentUser) {

        log.info("Incoming Purchase Order Request: {}", request);
        log.info("Authenticated User: {}", currentUser.getEmail());

        PurchaseOrderResponse response = poService.create(request, currentUser);
        log.info("Created Purchase Order with ID: {}", response.id());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<PurchaseOrderResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request,
            @AuthenticationPrincipal User currentUser) {

        log.info("Updating status for Purchase Order ID: {} to {}", id, request.status());
        return ResponseEntity.ok(poService.updateStatus(id, request, currentUser));
    }

    @PatchMapping("/{id}/payment")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<PurchaseOrderResponse> updatePaymentStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePoPaymentStatusRequest request,
            @AuthenticationPrincipal User currentUser) {

        log.info("Updating payment status for Purchase Order ID: {} to {}", id, request.paymentStatus());
        return ResponseEntity.ok(poService.updatePaymentStatus(id, request.paymentStatus(), currentUser));
    }

    @PatchMapping("/{id}/supplier-status")
    @PreAuthorize("hasRole('SUPPLIER')")
    public ResponseEntity<PurchaseOrderResponse> supplierStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request,
            @AuthenticationPrincipal User currentUser) {

        log.info("Supplier {} updating status for Purchase Order ID: {} to {}",
                currentUser.getEmail(), id, request.status());
        return ResponseEntity.ok(poService.supplierDecision(id, request.status(), currentUser));
    }

    @PatchMapping("/{id}/supplier-modify")
    @PreAuthorize("hasRole('SUPPLIER')")
    public ResponseEntity<PurchaseOrderResponse> supplierModify(
            @PathVariable Long id,
            @Valid @RequestBody ModifyPurchaseOrderRequest request,
            @AuthenticationPrincipal User currentUser) {

        log.info("Supplier {} modifying Purchase Order ID: {} with request: {}",
                currentUser.getEmail(), id, request);
        return ResponseEntity.ok(poService.supplierModify(id, request, currentUser));
    }
}
