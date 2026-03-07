package com.noura.platform.commerce.dto;

public record ShiftPerformance(
        String cashier,
        double hours,
        double totalSales,
        double salesPerHour,
        double cashVariance
) {}
