package com.inventra.category;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    boolean existsByName(String name);
    Optional<Category> findByName(String name);

    // All root (main) categories with subcategories eagerly
    @Query("SELECT c FROM Category c LEFT JOIN FETCH c.subcategories WHERE c.parent IS NULL ORDER BY c.name")
    List<Category> findAllRootWithSubs();

    // All categories flat (for product dropdown)
    @Query("SELECT c FROM Category c LEFT JOIN FETCH c.parent ORDER BY c.name")
    List<Category> findAllWithParent();

    // Subcategories of a parent
    List<Category> findByParentIdOrderByName(Long parentId);
}
