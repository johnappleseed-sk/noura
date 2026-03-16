package com.noura.platform.dto.brand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateBrandRequest(
        @NotBlank @Size(max = 80) String code,
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 255) String slug,
        Boolean active
) {
}
