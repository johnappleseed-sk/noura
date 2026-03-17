package com.noura.shipping.provider;

import com.noura.shipping.exception.ShippingOperationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Resolves carrier adapters by requested carrier code.
 */
@Component
public class ShippingCarrierRegistry {

    private final List<ShippingCarrier> carriers;

    /**
     * Creates registry with all carrier adapters registered in Spring.
     *
     * @param carriers carrier adapters
     */
    public ShippingCarrierRegistry(List<ShippingCarrier> carriers) {
        this.carriers = carriers == null ? List.of() : List.copyOf(carriers);
    }

    /**
     * Resolves one carrier adapter, defaulting to the first registered carrier when omitted.
     *
     * @param carrierCode requested carrier code
     * @return matching carrier adapter
     */
    public ShippingCarrier resolve(String carrierCode) {
        if (carriers.isEmpty()) {
            throw new ShippingOperationException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "SHIPPING_CARRIER_NOT_CONFIGURED",
                    "No shipping carrier adapters are configured"
            );
        }
        if (carrierCode == null || carrierCode.isBlank()) {
            return carriers.get(0);
        }
        return carriers.stream()
                .filter(carrier -> carrier.supports(carrierCode))
                .findFirst()
                .orElseThrow(() -> new ShippingOperationException(
                        HttpStatus.BAD_REQUEST,
                        "SHIPPING_CARRIER_UNSUPPORTED",
                        "Unsupported shipping carrier: " + carrierCode.trim()
                ));
    }

    /**
     * Resolves all carrier adapters, or one filtered carrier when code is supplied.
     *
     * @param carrierCode optional carrier filter
     * @return carrier list
     */
    public List<ShippingCarrier> resolveAll(String carrierCode) {
        if (carrierCode == null || carrierCode.isBlank()) {
            return carriers;
        }
        List<ShippingCarrier> filtered = carriers.stream()
                .filter(carrier -> carrier.supports(carrierCode))
                .toList();
        if (filtered.isEmpty()) {
            throw new ShippingOperationException(
                    HttpStatus.BAD_REQUEST,
                    "SHIPPING_CARRIER_UNSUPPORTED",
                    "Unsupported shipping carrier: " + carrierCode.trim()
            );
        }
        return filtered;
    }
}
