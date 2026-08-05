package com.inventra.aiinsights;

import com.inventra.inventory.InventoryRepository;
import com.inventra.product.Product;
import com.inventra.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AiInsightsService {

    private final DemandPredictionRepository  predictionRepository;
    private final ReorderSuggestionRepository suggestionRepository;
    private final ProductRepository           productRepository;
    private final InventoryRepository         inventoryRepository;

    @Transactional(readOnly = true)
    public List<PredictionResponse> getPredictions() {
        return predictionRepository.findAllWithProduct().stream()
                .map(PredictionResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<SuggestionResponse> getActiveSuggestions() {
        return suggestionRepository.findActiveWithProduct().stream()
                .map(SuggestionResponse::from).toList();
    }

    public List<PredictionResponse> runPredictions() {
        LocalDate nextMonth = LocalDate.now().plusMonths(1);
        for (Product product : productRepository.findAll()) {
            int predicted = Math.max(product.getReorderLevel() * 2, 10);
            BigDecimal confidence = BigDecimal.valueOf(0.72).setScale(2, RoundingMode.HALF_UP);
            predictionRepository.save(DemandPrediction.builder()
                    .product(product)
                    .predictedDemand(predicted)
                    .predictionDate(nextMonth)
                    .confidenceScore(confidence)
                    .build());
        }
        return getPredictions();
    }

    public List<SuggestionResponse> runReorderSuggestions() {
        inventoryRepository.findLowStock().forEach(inv -> {
            int suggested = inv.getProduct().getReorderLevel() * 3;
            suggestionRepository.save(ReorderSuggestion.builder()
                    .product(inv.getProduct())
                    .suggestedQuantity(suggested)
                    .reason("Stock (" + inv.getCurrentStock() + ") is below reorder level ("
                            + inv.getProduct().getReorderLevel() + "). Suggested: " + suggested + " units.")
                    .build());
        });
        inventoryRepository.findOutOfStock().forEach(inv -> {
            int suggested = inv.getProduct().getReorderLevel() * 3;
            suggestionRepository.save(ReorderSuggestion.builder()
                    .product(inv.getProduct())
                    .suggestedQuantity(suggested)
                    .reason("Out of stock. Urgent reorder of " + suggested + " units recommended.")
                    .build());
        });
        return getActiveSuggestions();
    }

    public void actionSuggestion(Long id) {
        suggestionRepository.findById(id).ifPresent(s -> s.setIsActioned(true));
    }
}
