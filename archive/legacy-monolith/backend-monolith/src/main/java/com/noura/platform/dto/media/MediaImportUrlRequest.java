package com.noura.platform.dto.media;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MediaImportUrlRequest(
        @NotBlank
        @Size(max = 2048)
        String url
) {
}
