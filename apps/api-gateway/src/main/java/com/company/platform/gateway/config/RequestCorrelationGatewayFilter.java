package com.company.platform.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Adds correlation IDs and one structured access log line to every gateway request.
 */
@Component
public class RequestCorrelationGatewayFilter implements GlobalFilter, Ordered {

    /**
     * Shared correlation header name propagated through the platform.
     */
    public static final String HEADER = "X-Correlation-ID";

    private static final Logger log = LoggerFactory.getLogger(RequestCorrelationGatewayFilter.class);
    private static final int MAX_CORRELATION_LENGTH = 128;

    /**
     * Runs before claim-forwarding so downstream handlers always see the correlation header.
     *
     * @return filter order
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 50;
    }

    /**
     * Propagates or generates a correlation ID, then logs request completion metadata.
     *
     * @param exchange current exchange
     * @param chain gateway filter chain
     * @return downstream completion signal
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String correlationId = resolveCorrelationId(exchange.getRequest().getHeaders().getFirst(HEADER));
        long startedAt = System.nanoTime();
        AtomicReference<Throwable> failure = new AtomicReference<>();

        HttpHeaders propagatedHeaders = new HttpHeaders();
        propagatedHeaders.putAll(exchange.getRequest().getHeaders());
        propagatedHeaders.set(HEADER, correlationId);

        // Spring Cloud Gateway exposes read-only request headers here, so use a decorator with copied headers.
        ServerHttpRequest request = new ServerHttpRequestDecorator(exchange.getRequest()) {
            @Override
            public HttpHeaders getHeaders() {
                return propagatedHeaders;
            }
        };
        ServerWebExchange correlatedExchange = exchange.mutate().request(request).build();
        correlatedExchange.getResponse().getHeaders().set(HEADER, correlationId);

        return chain.filter(correlatedExchange)
                .doOnError(failure::set)
                .doFinally(signalType -> logRequest(correlatedExchange, correlationId, startedAt, failure.get()));
    }

    /**
     * Writes one structured access log entry with timing and HTTP outcome.
     *
     * @param exchange completed exchange
     * @param correlationId resolved correlation ID
     * @param startedAt request start timestamp in nanoseconds
     * @param failure terminal error when one occurred
     */
    private void logRequest(
            ServerWebExchange exchange,
            String correlationId,
            long startedAt,
            Throwable failure
    ) {
        HttpStatusCode statusCode = exchange.getResponse().getStatusCode();
        int status = statusCode != null ? statusCode.value() : (failure == null ? 200 : 500);
        long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);

        log.info(
                "http_request correlationId={} method={} path={} status={} durationMs={}",
                correlationId,
                exchange.getRequest().getMethod(),
                exchange.getRequest().getPath().pathWithinApplication().value(),
                status,
                durationMs
        );
    }

    /**
     * Accepts a caller-supplied correlation ID when it is safe to propagate.
     *
     * @param headerValue inbound correlation header
     * @return normalized correlation ID
     */
    private String resolveCorrelationId(String headerValue) {
        if (headerValue == null || headerValue.isBlank()) {
            return UUID.randomUUID().toString();
        }
        String normalized = headerValue.trim();
        if (normalized.length() > MAX_CORRELATION_LENGTH) {
            return UUID.randomUUID().toString();
        }
        return normalized;
    }
}
