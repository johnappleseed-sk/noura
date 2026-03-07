package com.noura.platform.dto.product;

import java.util.UUID;

public record ProductMediaDto(
        UUID id,
        String mediaType,
        String url,
        int sortOrder,
        boolean primary
) {
}
