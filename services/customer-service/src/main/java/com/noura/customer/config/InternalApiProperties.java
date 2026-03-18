package com.noura.customer.config;

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
     * Shared internal API key. When blank, internal lookup endpoints are open in local/dev mode.
     */
    private String apiKey;
}
