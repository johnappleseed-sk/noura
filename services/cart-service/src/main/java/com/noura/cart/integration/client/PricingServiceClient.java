package com.noura.cart.integration.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.noura.cart.exception.CartOperationException;
import com.noura.cart.integration.PricingGateway;
import com.noura.cart.integration.model.PricingSnapshot;
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
public class PricingServiceClient implements PricingGateway {

    private static final ParameterizedTypeReference<RemoteApiEnvelope<PricingPayload>> PRICING_RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;

    /**
     * Creates pricing REST adapter.
     *
     * @param builder rest client builder
     * @param baseUrl pricing service base URL
     */
    public PricingServiceClient(
            RestClient.Builder builder,
            @Value("${services.pricing.base-url:http://localhost:8087}") String baseUrl
    ) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<PricingSnapshot> resolvePrice(UUID productId, UUID storeId) {
        try {
            RemoteApiEnvelope<PricingPayload> envelope = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/pricing/v1/prices/products/{productId}")
                            .queryParamIfPresent("storeId", Optional.ofNullable(storeId))
                            .build(productId))
                    .retrieve()
                    .body(PRICING_RESPONSE_TYPE);

            if (envelope == null || !Boolean.TRUE.equals(envelope.success()) || envelope.data() == null) {
                return Optional.empty();
            }
            PricingPayload payload = envelope.data();
            return Optional.of(new PricingSnapshot(
                    payload.productId(),
                    payload.effectivePrice(),
                    payload.currencyCode()
            ));
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            }
            log.warn("Pricing service call failed for product {}: status={} body={}",
                    productId, ex.getStatusCode().value(), ex.getResponseBodyAsString());
            throw new CartOperationException(
                    HttpStatus.BAD_GATEWAY,
                    "PRICING_SERVICE_ERROR",
                    "Pricing resolution is temporarily unavailable"
            );
        } catch (ResourceAccessException ex) {
            log.warn("Pricing service is unreachable for product {}: {}", productId, ex.getMessage());
            throw new CartOperationException(
                    HttpStatus.BAD_GATEWAY,
                    "PRICING_SERVICE_UNREACHABLE",
                    "Pricing resolution is temporarily unavailable"
            );
        }
    }

    /**
     * Generic remote API envelope.
     *
     * @param success success flag
     * @param message response message
     * @param data response payload
     * @param error error payload
     * @param <T> payload type
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RemoteApiEnvelope<T>(
            Boolean success,
            String message,
            T data,
            RemoteError error
    ) {
    }

    /**
     * Remote API error model.
     *
     * @param code stable code
     * @param detail detail message
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RemoteError(String code, String detail) {
    }

    /**
     * Pricing payload model returned by pricing-service.
     *
     * @param productId product identifier
     * @param currencyCode currency code
     * @param effectivePrice effective price
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PricingPayload(
            UUID productId,
            String currencyCode,
            BigDecimal effectivePrice
    ) {
    }
}
