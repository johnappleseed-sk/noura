package com.noura.platform.dto.carousel;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CarouselReorderItemRequest(
        @NotNull(message = "id is required")
        UUID id,

        @NotNull(message = "position is required")
        Integer position
) {
}
