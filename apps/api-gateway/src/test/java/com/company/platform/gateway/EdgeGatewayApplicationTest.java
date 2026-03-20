package com.company.platform.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit coverage for gateway startup metadata helpers.
 */
class EdgeGatewayApplicationTest {

    /**
     * Ensures the default local/dev topology is labeled as explicit URI routing.
     */
    @Test
    void resolvesExplicitUrisWhenDiscoveryIsDisabled() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.cloud.discovery.enabled", "false")
                .withProperty("spring.cloud.gateway.discovery.locator.enabled", "false");

        assertThat(EdgeGatewayApplication.resolveServiceResolutionMode(environment))
                .isEqualTo("explicit-uris");
    }

    /**
     * Ensures discovery-based routing is called out when gateway discovery is enabled.
     */
    @Test
    void resolvesDiscoveryWhenDiscoveryRoutingIsEnabled() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.cloud.discovery.enabled", "true")
                .withProperty("spring.cloud.gateway.discovery.locator.enabled", "true");

        assertThat(EdgeGatewayApplication.resolveServiceResolutionMode(environment))
                .isEqualTo("discovery");
    }
}
