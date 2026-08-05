package com.inventra.aiinsights;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ai-insights")
@RequiredArgsConstructor
public class AiInsightsController {

    private final AiInsightsService aiInsightsService;

    @GetMapping("/predictions")
    public ResponseEntity<List<PredictionResponse>> getPredictions() {
        return ResponseEntity.ok(aiInsightsService.getPredictions());
    }

    @GetMapping("/suggestions")
    public ResponseEntity<List<SuggestionResponse>> getSuggestions() {
        return ResponseEntity.ok(aiInsightsService.getActiveSuggestions());
    }

    @PostMapping("/predictions/run")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<List<PredictionResponse>> runPredictions() {
        return ResponseEntity.ok(aiInsightsService.runPredictions());
    }

    @PostMapping("/suggestions/run")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<List<SuggestionResponse>> runSuggestions() {
        return ResponseEntity.ok(aiInsightsService.runReorderSuggestions());
    }

    @PatchMapping("/suggestions/{id}/action")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<Void> actionSuggestion(@PathVariable Long id) {
        aiInsightsService.actionSuggestion(id);
        return ResponseEntity.noContent().build();
    }
}
