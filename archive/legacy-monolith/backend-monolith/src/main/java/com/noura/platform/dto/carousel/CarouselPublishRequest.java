package com.noura.platform.dto.carousel;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record CarouselPublishRequest(
        @NotNull(message = "published is required")
        Boolean published,
        Instant startAt,
        Instant endAt
) {
}
