package com.inventra.invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List; import java.util.Optional;
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findBySaleId(Long saleId);
    @Query("SELECT i FROM Invoice i ORDER BY i.createdAt DESC")
    List<Invoice> findAllOrderByCreatedAtDesc();
}
