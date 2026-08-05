package com.inventra.supplier;

import com.inventra.auth.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;

    // ── Supplier CRUD ────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<List<SupplierResponse>> getAll() {
        return ResponseEntity.ok(supplierService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SupplierResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(supplierService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<SupplierResponse> create(@Valid @RequestBody SupplierRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(supplierService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<SupplierResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody SupplierRequest req) {
        return ResponseEntity.ok(supplierService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        supplierService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ── My Profile (SUPPLIER role) ───────────────────────────────────────────

    @GetMapping("/me")
    @PreAuthorize("hasRole('SUPPLIER')")
    public ResponseEntity<SupplierResponse> getMyProfile(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(supplierService.getMyProfile(user.getEmail()));
    }

    // ── Warehouse Stock ──────────────────────────────────────────────────────

    /** Public GET - any authenticated user can view a supplier's warehouse */
    @GetMapping("/{id}/warehouse")
    public ResponseEntity<List<SupplierWarehouseStockResponse>> getWarehouse(@PathVariable Long id) {
        return ResponseEntity.ok(supplierService.getWarehouseStock(id));
    }

    /** Admin/Manager read - explicit endpoint */
    @GetMapping("/{id}/warehouse/admin")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<List<SupplierWarehouseStockResponse>> getWarehouseForAdmin(@PathVariable Long id) {
        return ResponseEntity.ok(supplierService.getWarehouseStock(id));
    }

    /** Supplier updates their own warehouse */
    @GetMapping("/me/warehouse")
    @PreAuthorize("hasRole('SUPPLIER')")
    public ResponseEntity<List<SupplierWarehouseStockResponse>> getMyWarehouse(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(supplierService.getMyWarehouseStock(user.getEmail()));
    }

    @PutMapping("/me/warehouse")
    @PreAuthorize("hasRole('SUPPLIER')")
    public ResponseEntity<SupplierWarehouseStockResponse> upsertMyWarehouse(
            @Valid @RequestBody WarehouseStockRequest req,
            @AuthenticationPrincipal User user) {
        Supplier supplier = supplierService.findByEmail(user.getEmail())
            .orElseThrow(() -> new IllegalArgumentException(
                "No supplier profile linked to your account."));
        return ResponseEntity.ok(
            supplierService.upsertWarehouseStock(supplier.getId(), req, user.getEmail(), false));
    }

    /** Admin/Manager can also update warehouse stock on behalf of a supplier */
    @PutMapping("/{id}/warehouse")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<SupplierWarehouseStockResponse> upsertWarehouseAdmin(
            @PathVariable Long id,
            @Valid @RequestBody WarehouseStockRequest req,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(
            supplierService.upsertWarehouseStock(id, req, user.getEmail(), true));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<List<SupplierResponse>> search(@RequestParam(required = false) String name) {
        return ResponseEntity.ok(supplierService.getAll());
    }
}
