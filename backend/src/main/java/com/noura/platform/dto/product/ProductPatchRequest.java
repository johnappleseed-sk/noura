package com.noura.platform.dto.product;

import java.util.Map;

public record ProductPatchRequest(
        String shortDescription,
        String longDescription,
        Map<String, Object> attributes,
        ProductSeoRequest seo,
        Boolean active,
        Boolean allowBackorder
) {
}
