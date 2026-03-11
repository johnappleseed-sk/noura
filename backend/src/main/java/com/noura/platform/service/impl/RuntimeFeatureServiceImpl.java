package com.noura.platform.service.impl;

import com.noura.platform.commerce.fulfillment.application.StorefrontFulfillmentService;
import com.noura.platform.commerce.orders.application.StorefrontOrderService;
import com.noura.platform.commerce.payments.application.StorefrontPaymentService;
import com.noura.platform.commerce.returns.application.ReturnService;
import com.noura.platform.dto.runtime.RuntimeFeaturesDto;
import com.noura.platform.service.RuntimeFeatureService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RuntimeFeatureServiceImpl implements RuntimeFeatureService {

    private static final String DISABLED_BY_PROFILE_MESSAGE = "Legacy storefront module is not active in the current runtime profile.";

    private final Environment environment;
    private final ObjectProvider<StorefrontOrderService> storefrontOrderServiceProvider;
    private final ObjectProvider<StorefrontPaymentService> storefrontPaymentServiceProvider;
    private final ObjectProvider<StorefrontFulfillmentService> storefrontFulfillmentServiceProvider;
    private final ObjectProvider<ReturnService> storefrontReturnServiceProvider;

    /**
     * Executes describe runtime features.
     *
     * @return The result of describe runtime features.
     */
    @Override
    public RuntimeFeaturesDto describeRuntimeFeatures() {
        boolean legacyStorefrontProfileActive = Arrays.asList(environment.getActiveProfiles()).contains("legacy-storefront");
        boolean storefrontOrdersEnabled = storefrontOrderServiceProvider.getIfAvailable() != null;
        boolean storefrontPaymentsEnabled = storefrontPaymentServiceProvider.getIfAvailable() != null;
        boolean storefrontFulfillmentEnabled = storefrontFulfillmentServiceProvider.getIfAvailable() != null;
        boolean storefrontReturnsEnabled = storefrontReturnServiceProvider.getIfAvailable() != null;

        Map<String, Boolean> features = new LinkedHashMap<>();
        features.put("runtime.profile.legacyStorefront", legacyStorefrontProfileActive);
        features.put("admin.refundWorkflowOnOrders", true);
        features.put("storefront.orderCancellation", storefrontOrdersEnabled);
        features.put("storefront.postCheckoutPayments", storefrontPaymentsEnabled);
        features.put("storefront.fulfillmentTracking", storefrontFulfillmentEnabled);
        features.put("storefront.returns", storefrontReturnsEnabled);

        Map<String, String> messages = new LinkedHashMap<>();
        messages.put("storefront.orderCancellation", storefrontOrdersEnabled ? "enabled" : DISABLED_BY_PROFILE_MESSAGE);
        messages.put("storefront.postCheckoutPayments", storefrontPaymentsEnabled ? "enabled" : DISABLED_BY_PROFILE_MESSAGE);
        messages.put("storefront.fulfillmentTracking", storefrontFulfillmentEnabled ? "enabled" : DISABLED_BY_PROFILE_MESSAGE);
        messages.put("storefront.returns", storefrontReturnsEnabled ? "enabled" : DISABLED_BY_PROFILE_MESSAGE);

        if (!legacyStorefrontProfileActive) {
            messages.put("runtime.profile.legacyStorefront", "legacy-storefront profile is not enabled.");
        } else {
            messages.put("runtime.profile.legacyStorefront", "legacy-storefront profile is enabled.");
        }

        return new RuntimeFeaturesDto("1.0", features, messages);
    }
}

