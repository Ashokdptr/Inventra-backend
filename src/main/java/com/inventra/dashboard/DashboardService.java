package com.inventra.dashboard;

import com.inventra.alert.AlertRepository;
import com.inventra.inventory.InventoryRepository;
import com.inventra.product.ProductRepository;
import com.inventra.purchaseorder.PurchaseOrder;
import com.inventra.purchaseorder.PurchaseOrderRepository;
import com.inventra.sale.SaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final ProductRepository       productRepository;
    private final InventoryRepository     inventoryRepository;
    private final SaleRepository          saleRepository;
    private final PurchaseOrderRepository poRepository;
    private final AlertRepository         alertRepository;

    public DashboardKpiResponse getKpis() {
        long total     = productRepository.count();
        long lowStock  = inventoryRepository.findLowStock().size();
        long outStock  = inventoryRepository.findOutOfStock().size();
        long alerts    = alertRepository.countByIsReadFalse();
        long pendingPO = poRepository.countByStatus(PurchaseOrder.Status.PENDING);
        long salesToday = saleRepository.countBySaleDateBetween(LocalDate.now(), LocalDate.now());

        BigDecimal invValue = inventoryRepository.findAllWithProduct().stream()
                .filter(i -> i.getCurrentStock() > 0)
                .map(i -> i.getProduct().getCostPrice()
                        .multiply(BigDecimal.valueOf(i.getCurrentStock())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // New products this month
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        long newThisMonth = productRepository.countByCreatedAtBetween(
                monthStart.atStartOfDay(), LocalDate.now().plusDays(1).atStartOfDay());

        // Revenue growth: this month vs last month
        LocalDate lastMonthStart = monthStart.minusMonths(1);
        LocalDate lastMonthEnd   = monthStart.minusDays(1);
        BigDecimal thisMonthRev  = saleRepository.sumTotalAmountByDateRange(monthStart, LocalDate.now());
        BigDecimal lastMonthRev  = saleRepository.sumTotalAmountByDateRange(lastMonthStart, lastMonthEnd);
        double revenueGrowth = 0;
        if (lastMonthRev != null && lastMonthRev.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal diff = (thisMonthRev != null ? thisMonthRev : BigDecimal.ZERO).subtract(lastMonthRev);
            revenueGrowth = diff.divide(lastMonthRev, 4, java.math.RoundingMode.HALF_UP)
                              .multiply(BigDecimal.valueOf(100)).doubleValue();
        }

        // Orders growth: this month vs last month
        long thisMonthOrders = saleRepository.countBySaleDateBetween(monthStart, LocalDate.now());
        long lastMonthOrders = saleRepository.countBySaleDateBetween(lastMonthStart, lastMonthEnd);
        double ordersGrowth  = lastMonthOrders > 0
            ? ((double)(thisMonthOrders - lastMonthOrders) / lastMonthOrders) * 100.0 : 0;

        return new DashboardKpiResponse(
                total, lowStock, outStock, alerts, invValue, pendingPO, salesToday,
                newThisMonth,
                Math.round(revenueGrowth * 10.0) / 10.0,
                Math.round(ordersGrowth  * 10.0) / 10.0);
    }
}
