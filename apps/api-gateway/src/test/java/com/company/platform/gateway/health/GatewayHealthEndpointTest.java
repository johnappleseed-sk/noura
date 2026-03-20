package com.company.platform.gateway.health;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Guards the gateway health contract for the explicit-URI local/dev topology.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class GatewayHealthEndpointTest {

    @Autowired
    private WebTestClient webTestClient;

    /**
     * Ensures actuator health does not expose unused discovery contributors when the
     * gateway is intentionally configured without a service registry.
     */
    @Test
    void healthOmitsUnusedDiscoveryContributors() {
        webTestClient
                .get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.components.discoveryComposite").doesNotExist()
                .jsonPath("$.components.reactiveDiscoveryClients").doesNotExist()
                .jsonPath("$.components.readinessState.status").isEqualTo("UP")
                .jsonPath("$.components.livenessState.status").isEqualTo("UP");
    }
}
