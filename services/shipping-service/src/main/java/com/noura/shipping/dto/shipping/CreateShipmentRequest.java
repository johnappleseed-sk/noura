package com.noura.shipping.dto.shipping;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Creates one shipment record for an order.
 *
 * @param orderId order identifier
 * @param carrierCode optional carrier code override
 * @param methodCode selected shipping method code
 * @param idempotencyKey optional idempotency key
 * @param signatureRequired whether signature is required on delivery
 * @param parcels parcel list used to build shipment weight and dimensions
 * @param metadata optional shipment metadata and rule scenarios
 */
public record CreateShipmentRequest(
        @NotNull(message = "orderId is required")
        UUID orderId,
        String carrierCode,
        @NotBlank(message = "methodCode is required")
        String methodCode,
        String idempotencyKey,
        Boolean signatureRequired,
        @NotEmpty(message = "parcels must contain at least one parcel")
        List<@Valid ParcelRequest> parcels,
        Map<String, Object> metadata
) {
}
