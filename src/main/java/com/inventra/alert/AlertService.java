package com.inventra.alert;

import com.inventra.common.exception.ResourceNotFoundException;
import com.inventra.product.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class AlertService {

    private final AlertRepository alertRepository;

    @Transactional(readOnly = true)
    public List<AlertResponse> getAll() {
        return alertRepository.findAllWithProduct().stream().map(AlertResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<AlertResponse> getUnread() {
        return alertRepository.findUnread().stream().map(AlertResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Long> countUnread() {
        return Map.of("unreadCount", alertRepository.countByIsReadFalse());
    }

    public AlertResponse markRead(Long id) {
        Alert alert = alertRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Alert", id));
        alert.setIsRead(true);
        return AlertResponse.from(alertRepository.save(alert));
    }

    public void markAllRead() {
        alertRepository.findUnread().forEach(a -> a.setIsRead(true));
    }

    public void checkAndCreateAlert(Product product, int newStock) {
        if (newStock == 0) {
            alertRepository.save(Alert.builder()
                .product(product).alertType(Alert.AlertType.OUT_OF_STOCK)
                .message("'" + product.getName() + "' is out of stock. Immediate reorder required.")
                .build());
        } else if (newStock <= product.getReorderLevel()) {
            alertRepository.save(Alert.builder()
                .product(product).alertType(Alert.AlertType.LOW_STOCK)
                .message("'" + product.getName() + "' stock is low (" + newStock
                    + " units). Reorder level: " + product.getReorderLevel() + ".")
                .build());
        }
    }
}
