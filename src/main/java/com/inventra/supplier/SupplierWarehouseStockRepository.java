package com.inventra.supplier;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierWarehouseStockRepository extends JpaRepository<SupplierWarehouseStock, Long> {
    List<SupplierWarehouseStock> findBySupplierId(Long supplierId);
    Optional<SupplierWarehouseStock> findBySupplierIdAndProductId(Long supplierId, Long productId);
}
