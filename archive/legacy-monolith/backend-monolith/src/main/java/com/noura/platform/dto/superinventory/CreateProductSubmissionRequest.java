package com.noura.platform.dto.superinventory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;
import java.util.UUID;

public record CreateProductSubmissionRequest(
        @NotNull UUID merchantId,
        @NotNull UUID storeId,
        @NotBlank @Size(max = 255) String proposedName,
        @Size(max = 255) String proposedBrand,
        @Size(max = 80) String proposedCategoryCode,
        Map<String, Object> proposedAttributesJson,
        @Size(max = 64) String proposedBarcode,
        @Size(max = 120) String proposedSku
) {
}
