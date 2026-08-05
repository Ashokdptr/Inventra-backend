package com.inventra.aiinsights;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReorderSuggestionRepository extends JpaRepository<ReorderSuggestion, Long> {

    @Query("SELECT r FROM ReorderSuggestion r JOIN FETCH r.product WHERE r.isActioned = false ORDER BY r.createdAt DESC")
    List<ReorderSuggestion> findActiveWithProduct();
}
