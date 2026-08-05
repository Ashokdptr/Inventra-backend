package com.inventra.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AuditController {

    private final AuditService auditService;

    /** Paginated filtered search — main table */
    @GetMapping
    public ResponseEntity<Page<AuditLogResponse>> search(
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "25") int size) {
        return ResponseEntity.ok(
            auditService.search(module, severity, keyword, fromDate, toDate, page, Math.min(size, 100))
        );
    }

    /** Last 50 entries for live feed */
    @GetMapping("/recent")
    public ResponseEntity<List<AuditLogResponse>> recent() {
        return ResponseEntity.ok(auditService.recent());
    }

    /** Stats strip — total, today, this week, per-module counts */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(auditService.stats());
    }
}
