package com.noura.platform.dto.product;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record ProductSearchResultDto(
        UUID id,
        String name,
        String category,
        @JsonProperty("description_missing") boolean descriptionMissing,
        @JsonProperty("barcode_missing") boolean barcodeMissing,
        @JsonProperty("qr_missing") boolean qrMissing,
        @JsonProperty("mirror_status") String mirrorStatus
) {
}
