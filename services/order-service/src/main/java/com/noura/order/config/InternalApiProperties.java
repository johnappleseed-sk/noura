package com.noura.order.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Internal API property group for service-to-service endpoint protection.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.internal")
public class InternalApiProperties {

    /**
     * Shared internal API key. When blank, only role-based admin checks are enforced.
     */
    private String apiKey;
}

