package com.noura.shipping.dto.network;

import com.noura.shipping.domain.enums.StoreServiceType;
import com.noura.shipping.domain.enums.StoreStatus;
import com.noura.shipping.domain.enums.StoreType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Store create/update payload aligned with admin-web and legacy control-center flows.
 */
public record StoreRequest(
        String storeCode,
        @NotBlank String name,
        String slug,
        UUID merchantId,
        @NotNull StoreType type,
        StoreStatus status,
        String countryCode,
        String city,
        String addressLine1,
        String addressLine2,
        String postalCode,
        String contactEmail,
        String contactPhone,
        BigDecimal latitude,
        BigDecimal longitude,
        Boolean openNow,
        List<StoreServiceType> supportedServices
) {
}
