package com.noura.platform.commerce.dto;

public record SkuPerformance(
        String name,
        double revenue,
        int qty,
        double profit,
        double marginPercent
) {}
