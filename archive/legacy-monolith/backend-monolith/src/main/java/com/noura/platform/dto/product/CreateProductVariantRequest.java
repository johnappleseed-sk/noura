package com.noura.platform.dto.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateProductVariantRequest(
        @NotBlank @Size(max = 120) String sku,
        @NotBlank @Size(max = 255) String variantName,
        @Size(max = 64) String barcode,
        Boolean active
) {
}
