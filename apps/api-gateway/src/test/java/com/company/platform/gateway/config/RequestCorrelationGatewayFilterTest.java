package com.company.platform.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression tests for {@link RequestCorrelationGatewayFilter}.
 */
class RequestCorrelationGatewayFilterTest {

    /**
     * Verifies correlation propagation does not fail when the inbound request uses read-only headers.
     */
    @Test
    void filterAddsCorrelationIdWithoutMutatingReadOnlyHeaders() {
        RequestCorrelationGatewayFilter filter = new RequestCorrelationGatewayFilter();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/products").build());
        AtomicReference<String> forwardedCorrelationId = new AtomicReference<>();

        GatewayFilterChain chain = mutatedExchange -> {
            forwardedCorrelationId.set(
                    mutatedExchange.getRequest().getHeaders().getFirst(RequestCorrelationGatewayFilter.HEADER)
            );
            mutatedExchange.getResponse().setStatusCode(HttpStatus.OK);
            return mutatedExchange.getResponse().setComplete();
        };

        filter.filter(exchange, chain).block();

        assertThat(forwardedCorrelationId.get()).isNotBlank();
        assertThat(exchange.getResponse().getHeaders().getFirst(RequestCorrelationGatewayFilter.HEADER))
                .isEqualTo(forwardedCorrelationId.get());
    }
}
