package com.noura.platform.commerce.fulfillment.application;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Registry to manage and select shipping carrier implementations.
 */
@Service
public class ShippingCarrierRegistry {
    private final Map<String, ShippingCarrier> carriers;
    private final ShippingCarrier defaultCarrier;

    public ShippingCarrierRegistry(List<ShippingCarrier> carrierList) {
        this.carriers = carrierList.stream()
                .collect(Collectors.toMap(ShippingCarrier::getCarrierId, Function.identity()));

        // Default to stub carrier if available
        this.defaultCarrier = carriers.getOrDefault("stub", carrierList.isEmpty() ? null : carrierList.get(0));
    }

    /**
     * Get a carrier by ID.
     */
    public Optional<ShippingCarrier> getCarrier(String carrierId) {
        if (carrierId == null || carrierId.isBlank()) {
            return Optional.ofNullable(defaultCarrier);
        }
        return Optional.ofNullable(carriers.get(carrierId.toLowerCase()));
    }

    /**
     * Get the default shipping carrier.
     */
    public ShippingCarrier getDefaultCarrier() {
        return defaultCarrier;
    }

    /**
     * Get all enabled carriers.
     */
    public List<ShippingCarrier> getEnabledCarriers() {
        return carriers.values().stream()
                .filter(ShippingCarrier::isEnabled)
                .toList();
    }

    /**
     * Get all available carrier IDs and names.
     */
    public List<CarrierInfo> getAvailableCarriers() {
        return carriers.values().stream()
                .filter(ShippingCarrier::isEnabled)
                .map(c -> new CarrierInfo(c.getCarrierId(), c.getDisplayName()))
                .toList();
    }

    public record CarrierInfo(String carrierId, String displayName) {}
}
