package com.noura.platform.dto.contract;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.Map;

public record MerchantContractCreateRequest(
        @NotBlank String contractNumber,
        @NotNull LocalDate startDate,
        LocalDate endDate,
        String note,
        Map<String, Object> terms
) {
}

