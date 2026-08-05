package com.inventra.aiinsights;

import java.math.BigDecimal;

public record PredictionResponse(
        Long id, Long productId, String productName,
        Integer predictedDemand, String predictionDate,
        BigDecimal confidenceScore, String createdAt
) {
    public static PredictionResponse from(DemandPrediction d) {
        return new PredictionResponse(
                d.getId(), d.getProduct().getId(), d.getProduct().getName(),
                d.getPredictedDemand(),
                d.getPredictionDate() != null ? d.getPredictionDate().toString() : null,
                d.getConfidenceScore(),
                d.getCreatedAt() != null ? d.getCreatedAt().toString() : null
        );
    }
}
