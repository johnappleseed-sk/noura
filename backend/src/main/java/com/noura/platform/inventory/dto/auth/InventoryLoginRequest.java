package com.noura.platform.inventory.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InventoryLoginRequest(
        @NotBlank @Size(max = 255) String login,
        @NotBlank @Size(max = 255) String password
) {
}
