package com.inventra.invoice;
import java.math.BigDecimal; import java.util.List;
public record InvoiceResponse(
    Long id, String invoiceNumber, Long saleId,
    String customerName, String customerEmail, String customerPhone, String customerAddress,
    BigDecimal subtotal, BigDecimal taxRate, BigDecimal taxAmount, BigDecimal totalAmount,
    String notes, String status, String createdBy, String createdAt,
    List<InvoiceItemDto> items
) {
    public record InvoiceItemDto(String productName, Integer quantity, BigDecimal unitPrice, BigDecimal subtotal) {}
    public static InvoiceResponse from(Invoice inv) {
        List<InvoiceItemDto> items = inv.getSale() != null && inv.getSale().getItems() != null
            ? inv.getSale().getItems().stream().map(i -> new InvoiceItemDto(
                i.getProduct().getName(), i.getQuantity(), i.getUnitPrice(),
                i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))).toList()
            : List.of();
        return new InvoiceResponse(
            inv.getId(), inv.getInvoiceNumber(), inv.getSale() != null ? inv.getSale().getId() : null,
            inv.getCustomerName(), inv.getCustomerEmail(), inv.getCustomerPhone(), inv.getCustomerAddress(),
            inv.getSubtotal(), inv.getTaxRate(), inv.getTaxAmount(), inv.getTotalAmount(),
            inv.getNotes(), inv.getStatus() != null ? inv.getStatus().name() : null,
            inv.getCreatedBy() != null ? inv.getCreatedBy().getName() : null,
            inv.getCreatedAt() != null ? inv.getCreatedAt().toString() : null,
            items);
    }
}
