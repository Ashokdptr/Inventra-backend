package com.inventra.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    void deleteByProductId(Long productId);

    @Query("SELECT m FROM StockMovement m JOIN FETCH m.product LEFT JOIN FETCH m.createdBy WHERE m.product.id = :productId ORDER BY m.createdAt DESC")
    List<StockMovement> findByProductId(@Param("productId") Long productId);

    @Query("SELECT m FROM StockMovement m JOIN FETCH m.product LEFT JOIN FETCH m.createdBy ORDER BY m.createdAt DESC")
    List<StockMovement> findAllWithDetails();
}
