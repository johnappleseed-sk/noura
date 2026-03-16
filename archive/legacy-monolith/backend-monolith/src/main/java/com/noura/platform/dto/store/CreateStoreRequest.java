package com.noura.platform.dto.store;

import com.noura.platform.domain.enums.StoreStatus;
import com.noura.platform.domain.enums.StoreType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateStoreRequest(
        @NotBlank @Size(min = 4, max = 80) String storeCode,
        @NotBlank @Size(min = 2, max = 255) String name,
        @NotBlank @Size(min = 2, max = 255) String slug,
        @NotNull UUID merchantId,
        @NotNull StoreType type,
        @NotNull StoreStatus status,
        @Email @Size(max = 255) String contactEmail,
        @Size(max = 40) String contactPhone,
        @Size(max = 12) String countryCode,
        @NotBlank @Size(min = 2, max = 255) String city,
        @NotBlank @Size(max = 255) String addressLine1,
        @Size(max = 255) String addressLine2
) {
}
