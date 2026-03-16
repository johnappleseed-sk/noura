package com.noura.platform.commerce.dto;

public record CashierPerformance(
        String cashier,
        double revenue,
        int transactions,
        int items,
        double avgOrderValue,
        double itemsPerMinute
) {}
