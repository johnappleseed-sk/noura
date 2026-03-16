package com.noura.platform.dto.product;

import com.noura.platform.domain.enums.ProductStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreateProductRequest(
        @NotBlank @Size(max = 80) String productCode,
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 255) String slug,
        UUID brandId,
        @NotNull UUID categoryId,
        @Size(max = 600) String shortDescription,
        @Size(max = 5000) String longDescription,
        ProductStatus status,
        @Size(max = 40) String approvalStatus,
        List<@Valid CreateProductVariantRequest> variants
) {
}
