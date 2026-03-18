package com.noura.checkout.integration.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.noura.checkout.exception.CheckoutOperationException;
import com.noura.checkout.integration.model.RemoteApiEnvelope;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * REST adapter to pricing-service APIs.
 */
@Slf4j
@Component
public class PricingServiceClient {

    private static final String HEADER_CORRELATION = "X-Correlation-ID";

    private static final ParameterizedTypeReference<RemoteApiEnvelope<PricePayload>> PRICE_RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;

    /**
     * Creates pricing-service REST adapter.
     *
     * @param builder rest client builder
     * @param baseUrl pricing-service base URL
     */
    public PricingServiceClient(
            RestClient.Builder builder,
            @Value("${services.pricing.base-url:http://localhost:8087}") String baseUrl
    ) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    /**
     * Resolves effective price for one product and optional store scope.
     *
     * @param productId product identifier
     * @param storeId optional store scope
     * @param correlationId correlation ID for tracing
     * @return optional price payload
     */
    public Optional<PricePayload> resolvePrice(UUID productId, UUID storeId, String correlationId) {
        try {
            RemoteApiEnvelope<PricePayload> envelope = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/pricing/v1/prices/products/{productId}")
                            .queryParamIfPresent("storeId", Optional.ofNullable(storeId))
                            .build(productId))
                    .header(HEADER_CORRELATION, correlationId)
                    .retrieve()
                    .body(PRICE_RESPONSE_TYPE);
            if (envelope == null || !Boolean.TRUE.equals(envelope.success()) || envelope.data() == null) {
                return Optional.empty();
            }
            return Optional.of(envelope.data());
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            }
            log.warn("Pricing service call failed for product {}: status={} body={}",
                    productId, ex.getStatusCode().value(), ex.getResponseBodyAsString());
            throw new CheckoutOperationException(
                    HttpStatus.BAD_GATEWAY,
                    "PRICING_SERVICE_ERROR",
                    "Pricing resolution is temporarily unavailable"
            );
        } catch (ResourceAccessException ex) {
            log.warn("Pricing service unreachable for product {}: {}", productId, ex.getMessage());
            throw new CheckoutOperationException(
                    HttpStatus.BAD_GATEWAY,
                    "PRICING_SERVICE_UNREACHABLE",
                    "Pricing resolution is temporarily unavailable"
            );
        }
    }

    /**
     * Pricing payload returned by pricing-service.
     *
     * @param productId product identifier
     * @param currencyCode currency code
     * @param effectivePrice effective price
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PricePayload(
            UUID productId,
            String currencyCode,
            BigDecimal effectivePrice
    ) {
    }
}

