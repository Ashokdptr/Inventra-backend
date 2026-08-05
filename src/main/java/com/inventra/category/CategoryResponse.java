package com.inventra.category;

import java.util.List;

public record CategoryResponse(
    Long id,
    String name,
    String description,
    Long parentId,
    String parentName,
    List<SubcategoryItem> subcategories,
    String createdAt
) {
    public record SubcategoryItem(Long id, String name, String description) {}

    public static CategoryResponse from(Category c) {
        List<SubcategoryItem> subs = c.getSubcategories() == null ? List.of()
            : c.getSubcategories().stream()
                .map(s -> new SubcategoryItem(s.getId(), s.getName(), s.getDescription()))
                .toList();
        return new CategoryResponse(
            c.getId(),
            c.getName(),
            c.getDescription(),
            c.getParent() != null ? c.getParent().getId()   : null,
            c.getParent() != null ? c.getParent().getName() : null,
            subs,
            c.getCreatedAt() != null ? c.getCreatedAt().toString() : null
        );
    }
}
