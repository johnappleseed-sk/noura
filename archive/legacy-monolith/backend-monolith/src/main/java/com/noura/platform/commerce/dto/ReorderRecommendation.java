package com.noura.platform.commerce.dto;

public record ReorderRecommendation(
        String name,
        Integer stockQty,
        double avgDaily,
        double daysOfStock,
        boolean lowStock
) {}
