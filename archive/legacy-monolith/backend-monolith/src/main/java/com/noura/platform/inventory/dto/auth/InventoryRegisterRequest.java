package com.noura.platform.inventory.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InventoryRegisterRequest(
        @NotBlank @Size(min = 3, max = 120) String username,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 3, max = 180) String fullName,
        @NotBlank @Size(min = 8, max = 255) String password
) {
}
