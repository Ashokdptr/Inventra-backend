package com.inventra.dashboard;

import java.math.BigDecimal;

public record DashboardKpiResponse(
        long totalProducts,
        long lowStockCount,
        long outOfStockCount,
        long unreadAlerts,
        BigDecimal totalInventoryValue,
        long pendingPurchaseOrders,
        long totalSalesToday,
        // Extended KPIs for dashboard analytics
        long newProductsThisMonth,
        double revenueGrowth,
        double ordersGrowth
) {}
