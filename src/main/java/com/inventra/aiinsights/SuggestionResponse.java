package com.inventra.aiinsights;

public record SuggestionResponse(
        Long id, Long productId, String productName,
        Integer suggestedQuantity, String reason,
        Boolean isActioned, String createdAt
) {
    public static SuggestionResponse from(ReorderSuggestion r) {
        return new SuggestionResponse(
                r.getId(), r.getProduct().getId(), r.getProduct().getName(),
                r.getSuggestedQuantity(), r.getReason(),
                r.getIsActioned(),
                r.getCreatedAt() != null ? r.getCreatedAt().toString() : null
        );
    }
}
