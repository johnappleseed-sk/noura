package com.noura.shipping.dto.shipping;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Quote request for one selected shipping method.
 *
 * @param address destination address snapshot
 * @param cartSubtotal cart subtotal before shipping
 * @param currencyCode cart currency code
 * @param itemCount cart item count
 * @param totalWeightKg cart total weight in kilograms
 * @param carrierCode optional carrier filter
 * @param methodCode requested shipping method code
 * @param metadata optional rule or scenario metadata
 */
public record ShippingQuoteRequest(
        @NotNull(message = "address is required")
        @Valid
        AddressRequest address,
        @NotNull(message = "cartSubtotal is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "cartSubtotal must be non-negative")
        BigDecimal cartSubtotal,
        @NotBlank(message = "currencyCode is required")
        String currencyCode,
        @NotNull(message = "itemCount is required")
        @Min(value = 1, message = "itemCount must be at least 1")
        Integer itemCount,
        @NotNull(message = "totalWeightKg is required")
        @DecimalMin(value = "0.0001", message = "totalWeightKg must be positive")
        BigDecimal totalWeightKg,
        String carrierCode,
        @NotBlank(message = "methodCode is required")
        String methodCode,
        Map<String, Object> metadata
) {
}
