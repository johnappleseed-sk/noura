package com.noura.shipping.dto.shipping;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Query-model payload for shipping method discovery.
 */
@Getter
@Setter
public class ShippingMethodQueryRequest {

    /**
     * Destination city used for same-day and zoning rules.
     */
    private String city;

    /**
     * Destination state or province.
     */
    private String stateProvince;

    /**
     * Destination postal code.
     */
    private String postalCode;

    /**
     * Destination country code.
     */
    @NotBlank(message = "countryCode is required")
    private String countryCode;

    /**
     * Cart subtotal amount before shipping/tax.
     */
    @NotNull(message = "cartSubtotal is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "cartSubtotal must be non-negative")
    private BigDecimal cartSubtotal;

    /**
     * Currency code used by the cart.
     */
    @NotBlank(message = "currencyCode is required")
    private String currencyCode;

    /**
     * Total item count in the cart.
     */
    @NotNull(message = "itemCount is required")
    @Min(value = 1, message = "itemCount must be at least 1")
    private Integer itemCount;

    /**
     * Total cart weight in kilograms.
     */
    @NotNull(message = "totalWeightKg is required")
    @DecimalMin(value = "0.0001", message = "totalWeightKg must be positive")
    private BigDecimal totalWeightKg;

    /**
     * Optional carrier filter when a caller needs one carrier's options only.
     */
    private String carrierCode;
}
