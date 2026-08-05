package com.inventra.invoice;

import com.inventra.auth.User;
import com.inventra.common.exception.ResourceNotFoundException;
import com.inventra.sale.Sale;
import com.inventra.sale.SaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final SaleRepository    saleRepository;

    @Transactional(readOnly = true)
    public List<InvoiceResponse> getAll() {
        return invoiceRepository.findAllOrderByCreatedAtDesc()
            .stream().map(InvoiceResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getById(Long id) {
        return InvoiceResponse.from(invoiceRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Invoice", id)));
    }

    public InvoiceResponse generateFromSale(Long saleId, BigDecimal taxRate, String notes, User createdBy) {
        invoiceRepository.findBySaleId(saleId).ifPresent(existing -> {
            throw new IllegalArgumentException(
                "Invoice already exists for Sale #" + saleId + ": " + existing.getInvoiceNumber());
        });

        Sale sale = saleRepository.findById(saleId)
            .orElseThrow(() -> new ResourceNotFoundException("Sale", saleId));

        BigDecimal subtotal   = sale.getTotalAmount();
        BigDecimal rate       = taxRate != null ? taxRate : BigDecimal.ZERO;
        BigDecimal taxAmount  = subtotal.multiply(rate)
                                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal total      = subtotal.add(taxAmount);

        String invNumber = "INV-"
            + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
            + "-" + String.format("%04d", saleId);

        Invoice inv = Invoice.builder()
            .invoiceNumber(invNumber)
            .sale(sale)
            .customerName(sale.getCustomerName())
            .customerEmail(sale.getCustomerEmail())
            .customerPhone(sale.getCustomerPhone())
            .subtotal(subtotal)
            .taxRate(rate)
            .taxAmount(taxAmount)
            .totalAmount(total)
            .notes(notes)
            .status(Invoice.InvoiceStatus.ISSUED)
            .createdBy(createdBy)
            .build();

        return InvoiceResponse.from(invoiceRepository.save(inv));
    }

    public InvoiceResponse updateStatus(Long id, Invoice.InvoiceStatus status) {
        Invoice inv = invoiceRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Invoice", id));
        inv.setStatus(status);
        return InvoiceResponse.from(invoiceRepository.save(inv));
    }
}
