package com.noura.platform.commerce.payments.application;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Registry to manage and select payment gateway implementations.
 */
@Service
public class PaymentGatewayRegistry {
    private final Map<String, PaymentGateway> gateways;
    private final PaymentGateway defaultGateway;

    public PaymentGatewayRegistry(List<PaymentGateway> gatewayList) {
        this.gateways = gatewayList.stream()
                .collect(Collectors.toMap(PaymentGateway::getProviderId, Function.identity()));

        // Default to stub gateway if available
        this.defaultGateway = gateways.getOrDefault("stub", gatewayList.isEmpty() ? null : gatewayList.get(0));
    }

    /**
     * Get a gateway by provider ID.
     */
    public Optional<PaymentGateway> getGateway(String providerId) {
        if (providerId == null || providerId.isBlank()) {
            return Optional.ofNullable(defaultGateway);
        }
        return Optional.ofNullable(gateways.get(providerId.toLowerCase()));
    }

    /**
     * Get the default payment gateway.
     */
    public PaymentGateway getDefaultGateway() {
        return defaultGateway;
    }

    /**
     * Get all enabled gateways.
     */
    public List<PaymentGateway> getEnabledGateways() {
        return gateways.values().stream()
                .filter(PaymentGateway::isEnabled)
                .toList();
    }

    /**
     * Get all registered provider IDs.
     */
    public List<String> getAvailableProviders() {
        return gateways.values().stream()
                .filter(PaymentGateway::isEnabled)
                .map(PaymentGateway::getProviderId)
                .toList();
    }
}
