package com.inventra.category;

import com.inventra.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;

    // ── Full tree (root categories + their subcategories) ──────────────────
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAll() {
        return categoryRepository.findAllRootWithSubs().stream()
                .map(CategoryResponse::from).toList();
    }

    // ── Flat list with parentId/parentName (for product dropdowns) ──────────
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllFlat() {
        return categoryRepository.findAllWithParent().stream()
                .map(CategoryResponse::from).toList();
    }

    // ── Subcategories for a given parent ────────────────────────────────────
    @Transactional(readOnly = true)
    public List<CategoryResponse> getSubcategories(Long parentId) {
        return categoryRepository.findByParentIdOrderByName(parentId).stream()
                .map(CategoryResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse getById(Long id) {
        return categoryRepository.findById(id).map(CategoryResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
    }

    public CategoryResponse create(CategoryRequest req) {
        Category cat = new Category();
        cat.setName(req.name().trim());
        cat.setDescription(req.description());
        if (req.parentId() != null) {
            Category parent = categoryRepository.findById(req.parentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category", req.parentId()));
            // Prevent nesting beyond 1 level
            if (parent.getParent() != null)
                throw new IllegalArgumentException("Subcategories cannot have sub-subcategories.");
            cat.setParent(parent);
        }
        return CategoryResponse.from(categoryRepository.save(cat));
    }

    public CategoryResponse update(Long id, CategoryRequest req) {
        Category cat = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
        cat.setName(req.name().trim());
        cat.setDescription(req.description());
        if (req.parentId() != null) {
            if (req.parentId().equals(id))
                throw new IllegalArgumentException("A category cannot be its own parent.");
            Category parent = categoryRepository.findById(req.parentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category", req.parentId()));
            if (parent.getParent() != null)
                throw new IllegalArgumentException("Subcategories cannot have sub-subcategories.");
            cat.setParent(parent);
        } else {
            cat.setParent(null);
        }
        return CategoryResponse.from(categoryRepository.save(cat));
    }

    public void delete(Long id) {
        Category cat = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
        if (!cat.getSubcategories().isEmpty())
            throw new IllegalStateException("Cannot delete a category that has subcategories. Delete subcategories first.");
        categoryRepository.deleteById(id);
    }
}
