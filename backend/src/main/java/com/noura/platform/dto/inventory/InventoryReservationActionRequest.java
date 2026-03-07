package com.noura.platform.dto.inventory;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record InventoryReservationActionRequest(
        @NotNull UUID reservationId,
        String note
) {
}
