package com.noura.payment.provider;

import com.noura.payment.exception.PaymentOperationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Resolves provider adapters by requested provider code.
 */
@Component
public class PaymentProviderRegistry {

    private final List<PaymentProvider> providers;

    /**
     * Creates registry with all provider adapters registered in Spring.
     *
     * @param providers provider adapters
     */
    public PaymentProviderRegistry(List<PaymentProvider> providers) {
        this.providers = providers == null ? List.of() : List.copyOf(providers);
    }

    /**
     * Resolves a provider adapter.
     *
     * @param providerCode requested provider code
     * @return matching provider adapter
     */
    public PaymentProvider resolve(String providerCode) {
        if (providerCode == null || providerCode.isBlank()) {
            throw new PaymentOperationException(
                    HttpStatus.BAD_REQUEST,
                    "PAYMENT_PROVIDER_REQUIRED",
                    "Payment provider code is required"
            );
        }
        return providers.stream()
                .filter(provider -> provider.supports(providerCode))
                .findFirst()
                .orElseThrow(() -> new PaymentOperationException(
                        HttpStatus.BAD_REQUEST,
                        "PAYMENT_PROVIDER_UNSUPPORTED",
                        "Unsupported payment provider: " + providerCode.trim()
                ));
    }
}
