package com.noura.platform.dto.carousel;

import com.noura.platform.domain.enums.CarouselStatus;
import jakarta.validation.constraints.NotNull;

public record CarouselStatusUpdateRequest(
        @NotNull(message = "status is required")
        CarouselStatus status
) {
}
