package com.noura.platform.commerce.dto;

public record CategoryPerformance(
        String name,
        double revenue,
        double profit,
        double marginPercent
) {}
