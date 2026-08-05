package com.inventra.purchaseorder;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    // ✅ Fetch all purchase orders with supplier, items, products, and createdBy
    @Query("""
            SELECT DISTINCT po FROM PurchaseOrder po
            LEFT JOIN FETCH po.supplier
            LEFT JOIN FETCH po.items i
            LEFT JOIN FETCH i.product
            LEFT JOIN FETCH po.createdBy
            ORDER BY po.createdAt DESC
            """)
    List<PurchaseOrder> findAllWithDetails();

    // ✅ Paginated version for large datasets
    @Query("""
            SELECT DISTINCT po FROM PurchaseOrder po
            LEFT JOIN FETCH po.supplier
            LEFT JOIN FETCH po.items i
            LEFT JOIN FETCH i.product
            LEFT JOIN FETCH po.createdBy
            """)
    Page<PurchaseOrder> findAllWithDetails(Pageable pageable);

    // ✅ Fetch single purchase order with details by ID
    @Query("""
            SELECT po FROM PurchaseOrder po
            LEFT JOIN FETCH po.supplier
            LEFT JOIN FETCH po.items i
            LEFT JOIN FETCH i.product
            LEFT JOIN FETCH po.createdBy
            WHERE po.id = :id
            """)
    Optional<PurchaseOrder> findByIdWithDetails(@Param("id") Long id);

    // ✅ Find purchase orders by status
    List<PurchaseOrder> findByStatus(PurchaseOrder.Status status);

    // ✅ Count purchase orders by status
    long countByStatus(PurchaseOrder.Status status);
}
