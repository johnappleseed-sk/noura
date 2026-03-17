package com.noura.payment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for the built-in sandbox/mock payment provider.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.payments.sandbox")
public class SandboxProviderProperties {

    /**
     * Optional shared secret used for placeholder webhook signature validation.
     */
    private String webhookSecret;
}
