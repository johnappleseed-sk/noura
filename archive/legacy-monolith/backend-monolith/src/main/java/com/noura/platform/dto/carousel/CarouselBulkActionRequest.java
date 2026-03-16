package com.noura.platform.dto.carousel;

import com.noura.platform.domain.enums.CarouselBulkActionType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CarouselBulkActionRequest(
        @NotEmpty(message = "ids are required")
        List<UUID> ids,

        @NotNull(message = "action is required")
        CarouselBulkActionType action
) {
}
