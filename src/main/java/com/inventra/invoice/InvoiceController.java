package com.inventra.invoice;

import com.inventra.auth.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    public ResponseEntity<List<InvoiceResponse>> getAll() {
        return ResponseEntity.ok(invoiceService.getAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    public ResponseEntity<InvoiceResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(invoiceService.getById(id));
    }

    @PostMapping("/from-sale/{saleId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    public ResponseEntity<InvoiceResponse> generate(
            @PathVariable Long saleId,
            @RequestBody(required = false) Map<String, Object> body,
            @AuthenticationPrincipal User user) {

        BigDecimal tax   = null;
        String    notes  = null;
        if (body != null) {
            Object taxObj = body.get("taxRate");
            if (taxObj != null) tax = new BigDecimal(taxObj.toString());
            notes = (String) body.get("notes");
        }
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(invoiceService.generateFromSale(saleId, tax, notes, user));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<InvoiceResponse> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String status = body.get("status");
        if (status == null || status.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(invoiceService.updateStatus(id, Invoice.InvoiceStatus.valueOf(status)));
    }
}
