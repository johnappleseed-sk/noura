package com.noura.platform.commerce.dto;

public record CustomerRfm(
        String name,
        int recencyDays,
        int frequency,
        double monetary,
        int score
) {}
