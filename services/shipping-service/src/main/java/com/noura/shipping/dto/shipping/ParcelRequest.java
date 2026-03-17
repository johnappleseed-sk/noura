package com.noura.shipping.dto.shipping;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Parcel dimensions and weight used for shipment creation.
 *
 * @param quantity parcel quantity represented by this row
 * @param weightKg parcel weight in kilograms
 * @param lengthCm parcel length in centimeters
 * @param widthCm parcel width in centimeters
 * @param heightCm parcel height in centimeters
 */
public record ParcelRequest(
        @NotNull(message = "quantity is required")
        @Min(value = 1, message = "quantity must be at least 1")
        Integer quantity,
        @NotNull(message = "weightKg is required")
        @DecimalMin(value = "0.0001", message = "weightKg must be positive")
        BigDecimal weightKg,
        @DecimalMin(value = "0.0", inclusive = true, message = "lengthCm must be non-negative")
        BigDecimal lengthCm,
        @DecimalMin(value = "0.0", inclusive = true, message = "widthCm must be non-negative")
        BigDecimal widthCm,
        @DecimalMin(value = "0.0", inclusive = true, message = "heightCm must be non-negative")
        BigDecimal heightCm
) {
}
