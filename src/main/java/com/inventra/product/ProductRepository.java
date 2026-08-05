package com.inventra.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsBySku(String sku);
    Optional<Product> findBySku(String sku);

    // Count products created in a time range (for newProductsThisMonth)
    long countByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    // Eagerly fetch category + category.parent for parentCategoryName
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.category c LEFT JOIN FETCH c.parent LEFT JOIN FETCH p.supplier")
    List<Product> findAllWithDetails();

    @Query("""
        SELECT p FROM Product p
        LEFT JOIN FETCH p.category c
        LEFT JOIN FETCH c.parent
        LEFT JOIN FETCH p.supplier
        WHERE (
            :keyword IS NULL OR :keyword = '' OR
            LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
            LOWER(p.sku)  LIKE LOWER(CONCAT('%', :keyword, '%')) OR
            CAST(p.id AS string) = :keyword
        )
        AND (:categoryId IS NULL OR p.category.id = :categoryId)
        AND (:supplierId IS NULL OR p.supplier.id = :supplierId)
        """)
    List<Product> searchProducts(
        @Param("keyword") String keyword,
        @Param("categoryId") Long categoryId,
        @Param("supplierId") Long supplierId
    );
}
