package com.noura.shipping.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for the internal rule-based shipping carrier.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.shipping.rule-based")
public class RuleBasedCarrierProperties {

    /**
     * Stable carrier code exposed by the internal rule-based carrier.
     */
    private String carrierCode = "rule-based";

    /**
     * Human-readable carrier display name.
     */
    private String displayName = "Noura Rule-Based Shipping";

    /**
     * Cart subtotal threshold for free standard shipping.
     */
    private BigDecimal freeStandardThreshold = new BigDecimal("75.00");

    /**
     * Country code eligible for same-day delivery rules.
     */
    private String sameDayCountryCode = "KH";

    /**
     * City names that qualify for same-day delivery when other rules also pass.
     */
    private List<String> sameDayCities = new ArrayList<>(List.of("Phnom Penh"));
}
