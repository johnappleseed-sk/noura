package com.noura.platform.dto.submission;

import com.noura.platform.dto.product.ProductRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record ProductSubmissionCreateRequest(
        @NotNull @Valid ProductRequest product,
        String note
) {
}

