package com.noura.platform.dto.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProductGeneratorResponse {

    @JsonProperty("product_name")
    private final String productName;

    private final String description;

    private final String barcode;

    @JsonProperty("qr_code_base64")
    private final String qrCodeBase64;
}
