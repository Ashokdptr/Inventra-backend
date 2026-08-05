package com.inventra.supplier;

import com.inventra.audit.AuditService;
import com.inventra.common.exception.ResourceNotFoundException;
import com.inventra.product.Product;
import com.inventra.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class SupplierService {

    private final SupplierRepository               supplierRepository;
    private final SupplierWarehouseStockRepository warehouseStockRepository;
    private final ProductRepository                productRepository;
    private final AuditService                     auditService;

    // ── Supplier CRUD ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<SupplierResponse> getAll() {
        return supplierRepository.findAll().stream().map(SupplierResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public SupplierResponse getById(Long id) {
        return SupplierResponse.from(supplierRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Supplier", id)));
    }

    public SupplierResponse create(SupplierRequest req) {
        validateRequest(req);
        if (req.phone() != null && !req.phone().isBlank()) {
            if (supplierRepository.existsByPhoneAndIdNot(req.phone().trim(), -1L)) {
                throw new IllegalArgumentException("A supplier with this phone number already exists.");
            }
        }
        Supplier s = Supplier.builder()
            .name(req.name().trim())
            .email(req.email())
            .phone(req.phone())
            .address(req.address())
            .contactName(req.contactName())
            .build();
        Supplier saved = supplierRepository.save(s);
        auditService.log(AuditService.MOD_SUPPLIER, "CREATED",
                "New supplier added: " + saved.getName() + " | Email: " + saved.getEmail(),
                "ADMIN/MANAGER", "ADMIN", saved.getId(), null, AuditService.SEV_SUCCESS);
        return SupplierResponse.from(saved);
    }

    public SupplierResponse update(Long id, SupplierRequest req) {
        validateRequest(req);
        if (req.phone() != null && !req.phone().isBlank()) {
            if (supplierRepository.existsByPhoneAndIdNot(req.phone().trim(), id)) {
                throw new IllegalArgumentException("A supplier with this phone number already exists.");
            }
        }
        Supplier s = supplierRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Supplier", id));
        s.setName(req.name().trim());
        s.setEmail(req.email());
        s.setPhone(req.phone());
        s.setAddress(req.address());
        s.setContactName(req.contactName());
        Supplier updated = supplierRepository.save(s);
        auditService.log(AuditService.MOD_SUPPLIER, "UPDATED",
                "Supplier updated: " + updated.getName(),
                "ADMIN/MANAGER", "ADMIN", updated.getId(), null, AuditService.SEV_INFO);
        return SupplierResponse.from(updated);
    }

    public void delete(Long id) {
        if (!supplierRepository.existsById(id)) {
            throw new ResourceNotFoundException("Supplier", id);
        }
        supplierRepository.deleteById(id);
        auditService.log(AuditService.MOD_SUPPLIER, "DELETED",
                "Supplier deleted: ID #" + id,
                "ADMIN/MANAGER", "ADMIN", id, null, AuditService.SEV_WARN);
    }

    // ── My Profile (SUPPLIER role) ───────────────────────────────────────────

    @Transactional(readOnly = true)
    public SupplierResponse getMyProfile(String email) {
        return SupplierResponse.from(supplierRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new IllegalArgumentException(
                "No supplier profile linked to email: " + email)));
    }

    @Transactional(readOnly = true)
    public Optional<Supplier> findByEmail(String email) {
        return supplierRepository.findByEmailIgnoreCase(email);
    }

    // ── Warehouse Stock ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<SupplierWarehouseStockResponse> getWarehouseStock(Long supplierId) {
        supplierRepository.findById(supplierId)
            .orElseThrow(() -> new ResourceNotFoundException("Supplier", supplierId));
        return warehouseStockRepository.findBySupplierId(supplierId)
            .stream().map(SupplierWarehouseStockResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<SupplierWarehouseStockResponse> getMyWarehouseStock(String email) {
        Supplier supplier = supplierRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new IllegalArgumentException(
                "No supplier profile linked to your account."));
        return warehouseStockRepository.findBySupplierId(supplier.getId())
            .stream().map(SupplierWarehouseStockResponse::from).toList();
    }

    public SupplierWarehouseStockResponse upsertWarehouseStock(
            Long supplierId, WarehouseStockRequest req,
            String currentUserEmail, boolean isManager) {

        Supplier supplier = supplierRepository.findById(supplierId)
            .orElseThrow(() -> new ResourceNotFoundException("Supplier", supplierId));

        if (!isManager) {
            if (supplier.getEmail() == null
                    || !supplier.getEmail().equalsIgnoreCase(currentUserEmail)) {
                throw new IllegalArgumentException("You can only update your own warehouse stock.");
            }
        }

        Product product = productRepository.findById(req.productId())
            .orElseThrow(() -> new ResourceNotFoundException("Product", req.productId()));

        SupplierWarehouseStock stock = warehouseStockRepository
            .findBySupplierIdAndProductId(supplierId, req.productId())
            .orElseGet(() -> SupplierWarehouseStock.builder()
                .supplier(supplier)
                .product(product)
                .availableQuantity(0)
                .build());

        stock.setAvailableQuantity(req.availableQuantity());
        if (req.costPrice() != null)    stock.setCostPrice(req.costPrice());
        if (req.supplierSku() != null && !req.supplierSku().isBlank()) {
            stock.setSupplierSku(req.supplierSku().trim());
        }

        return SupplierWarehouseStockResponse.from(warehouseStockRepository.save(stock));
    }

    // ── Called on SUPPLIER registration ─────────────────────────────────────

    public void ensureSupplierProfile(String name, String email, String phone, String companyName) {
        if (email == null || supplierRepository.findByEmailIgnoreCase(email).isPresent()) return;
        if (phone != null && !phone.isBlank() && supplierRepository.existsByPhone(phone.trim())) {
            phone = null;
        }
        Supplier s = Supplier.builder()
            .name(companyName != null ? companyName : name)
            .email(email)
            .phone(phone)
            .address("")
            .contactName(name)
            .build();
        supplierRepository.save(s);
    }

    // ── Validation ───────────────────────────────────────────────────────────

    private void validateRequest(SupplierRequest req) {
        if (req.name() == null || req.name().trim().length() < 2) {
            throw new IllegalArgumentException("Supplier name must be at least 2 characters.");
        }
        if (req.email() != null && !req.email().isBlank()
                && !req.email().matches("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$")) {
            throw new IllegalArgumentException("Invalid email address.");
        }
    }
}
