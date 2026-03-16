package com.noura.platform.commerce.dto;

public record MoverStat(
        String name,
        int qtySold,
        Integer stockQty,
        Double daysOfStock
) {}
