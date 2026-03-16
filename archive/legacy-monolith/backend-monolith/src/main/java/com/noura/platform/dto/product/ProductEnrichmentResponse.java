package com.noura.platform.dto.product;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record ProductEnrichmentResponse(
        UUID id,
        @JsonProperty("product_name") String productName,
        String description,
        String barcode,
        @JsonProperty("qr_code") String qrCode,
        @JsonProperty("barcode_image_url") String barcodeImageUrl,
        @JsonProperty("qr_image_url") String qrImageUrl,
        @JsonProperty("description_generated") boolean descriptionGenerated,
        @JsonProperty("barcode_generated") boolean barcodeGenerated,
        @JsonProperty("qr_generated") boolean qrGenerated,
        @JsonProperty("mirror_status") String mirrorStatus,
        @JsonProperty("mirror_warning") String mirrorWarning
) {
}
