package com.noura.platform.inventory.dto.batch;

import java.time.LocalDate;

public record BatchLotFilter(
        String productId,
        String status,
        LocalDate expiringBefore,
        LocalDate expiringAfter
) {
}
