package com.noura.platform.dto.inventory;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record InventoryCheckRequest(
        @NotEmpty List<@Valid InventoryCheckItemRequest> items
) {
}
