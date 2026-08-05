package com.inventra.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository repo;

    // ── Modules ──────────────────────────────────────────────────────────────
    public static final String MOD_AUTH       = "AUTH";
    public static final String MOD_SALE       = "SALE";
    public static final String MOD_PO         = "PURCHASE_ORDER";
    public static final String MOD_PRODUCT    = "PRODUCT";
    public static final String MOD_SUPPLIER   = "SUPPLIER";
    public static final String MOD_INVENTORY  = "INVENTORY";
    public static final String MOD_USER       = "USER";
    public static final String MOD_PAYMENT    = "PAYMENT";
    public static final String MOD_CATEGORY   = "CATEGORY";

    // ── Severities ───────────────────────────────────────────────────────────
    public static final String SEV_INFO    = "INFO";
    public static final String SEV_SUCCESS = "SUCCESS";
    public static final String SEV_WARN    = "WARNING";
    public static final String SEV_DANGER  = "DANGER";

    // ── Core log method (async, new transaction so it never blocks/rolls back) ──
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String module, String action, String description,
                    String performedBy, String userRole, Long entityId,
                    String extraInfo, String severity, String ip) {
        try {
            AuditLog entry = AuditLog.builder()
                .module(module).action(action).description(description)
                .performedBy(performedBy).userRole(userRole)
                .entityId(entityId).extraInfo(extraInfo)
                .severity(severity).ipAddress(ip)
                .build();
            repo.save(entry);
        } catch (Exception e) {
            log.error("Audit log failed: {}", e.getMessage());
        }
    }

    // Convenience overload (no IP) — fully inlined so @Async applies
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String module, String action, String description,
                    String performedBy, String userRole, Long entityId,
                    String extraInfo, String severity) {
        try {
            AuditLog entry = AuditLog.builder()
                .module(module).action(action).description(description)
                .performedBy(performedBy).userRole(userRole)
                .entityId(entityId).extraInfo(extraInfo)
                .severity(severity).ipAddress(null)
                .build();
            repo.save(entry);
        } catch (Exception e) {
            log.error("Audit log failed: {}", e.getMessage());
        }
    }

    // ── Query methods ─────────────────────────────────────────────────────────

    public Page<AuditLogResponse> search(
            String module, String severity, String keyword,
            String fromDate, String toDate, int page, int size) {

        Pageable pageable = PageRequest.of(page, size);
        LocalDateTime from = fromDate != null && !fromDate.isBlank()
            ? LocalDate.parse(fromDate).atStartOfDay() : null;
        LocalDateTime to = toDate != null && !toDate.isBlank()
            ? LocalDate.parse(toDate).atTime(LocalTime.MAX) : null;

        String modFilter = (module   != null && !module.isBlank())   ? module   : null;
        String sevFilter = (severity != null && !severity.isBlank()) ? severity : null;
        String kwFilter  = (keyword  != null && !keyword.isBlank())  ? keyword  : null;

        return repo.search(modFilter, sevFilter, kwFilter, from, to, pageable)
                   .map(AuditLogResponse::from);
    }

    public List<AuditLogResponse> recent() {
        return repo.findTop50ByOrderByCreatedAtDesc()
                   .stream().map(AuditLogResponse::from).toList();
    }

    public Map<String, Object> stats() {
        long total   = repo.count();
        long today   = repo.countByCreatedAtAfter(LocalDate.now().atStartOfDay());
        long week    = repo.countByCreatedAtAfter(LocalDate.now().minusDays(7).atStartOfDay());

        List<Object[]> byModule = repo.countByModule();
        Map<String, Long> moduleMap = byModule.stream()
            .collect(Collectors.toMap(
                row -> (String) row[0],
                row -> (Long)   row[1]
            ));

        return Map.of(
            "total",    total,
            "today",    today,
            "thisWeek", week,
            "byModule", moduleMap
        );
    }
}
