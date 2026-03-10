package com.noura.platform.dto.carousel;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CarouselReorderRequest(
        @NotEmpty(message = "items are required")
        List<@Valid CarouselReorderItemRequest> items
) {
}
