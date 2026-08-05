package com.inventra.sale;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {

    @Query("""
        SELECT DISTINCT s FROM Sale s
        LEFT JOIN FETCH s.items i
        LEFT JOIN FETCH i.product p
        LEFT JOIN FETCH p.category cat
        LEFT JOIN FETCH cat.parent
        LEFT JOIN FETCH s.createdBy
        ORDER BY s.saleDate DESC
        """)
    List<Sale> findAllWithDetails();

    @Query("""
        SELECT s FROM Sale s
        LEFT JOIN FETCH s.items i
        LEFT JOIN FETCH i.product p
        LEFT JOIN FETCH p.category cat
        LEFT JOIN FETCH cat.parent
        LEFT JOIN FETCH s.createdBy
        WHERE s.id = :id
        """)
    Optional<Sale> findByIdWithDetails(@Param("id") Long id);

    @Query("""
        SELECT DISTINCT s FROM Sale s
        LEFT JOIN FETCH s.items i
        LEFT JOIN FETCH i.product p
        LEFT JOIN FETCH p.category cat
        LEFT JOIN FETCH cat.parent
        LEFT JOIN FETCH s.createdBy
        WHERE s.saleDate BETWEEN :from AND :to
        ORDER BY s.saleDate DESC
        """)
    List<Sale> findByDateRange(@Param("from") LocalDate from, @Param("to") LocalDate to);

    long countBySaleDateBetween(LocalDate from, LocalDate to);

    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM Sale s WHERE s.saleDate BETWEEN :from AND :to")
    BigDecimal sumTotalAmountByDateRange(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
