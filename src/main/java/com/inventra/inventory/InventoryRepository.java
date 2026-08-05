package com.inventra.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByProductId(Long productId);

    void deleteByProductId(Long productId);

    @Query("SELECT i FROM Inventory i JOIN FETCH i.product p WHERE i.currentStock > 0 AND i.currentStock <= p.reorderLevel")
    List<Inventory> findLowStock();

    @Query("SELECT i FROM Inventory i JOIN FETCH i.product WHERE i.currentStock = 0")
    List<Inventory> findOutOfStock();

    @Query("SELECT i FROM Inventory i JOIN FETCH i.product")
    List<Inventory> findAllWithProduct();
}
