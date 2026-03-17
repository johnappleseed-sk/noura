package com.noura.review.integration.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.noura.review.exception.ReviewOperationException;
import com.noura.review.integration.model.RemoteApiEnvelope;
import com.noura.review.service.model.ReviewRequestContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.UUID;

/**
 * REST adapter used by review-service to validate product identity and activity.
 */
@Slf4j
@Component
public class CatalogServiceClient {

    private static final String HEADER_SUBJECT = "X-Auth-Subject";
    private static final String HEADER_INTERNAL_API_KEY = "X-Internal-Api-Key";
    private static final String HEADER_CORRELATION = "X-Correlation-ID";

    private static final ParameterizedTypeReference<RemoteApiEnvelope<ProductPayload>> PRODUCT_RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;
    private final String internalApiKey;

    /**
     * Creates catalog-service REST adapter.
     *
     * @param builder rest client builder
     * @param baseUrl catalog-service base URL
     * @param internalApiKey optional shared internal API key
     */
    public CatalogServiceClient(
            RestClient.Builder builder,
            @Value("${services.catalog.base-url:http://localhost:8084}") String baseUrl,
            @Value("${services.catalog.internal-api-key:}") String internalApiKey
    ) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.internalApiKey = internalApiKey;
    }

    /**
     * Loads one product by ID while forwarding correlation and actor context.
     *
     * @param context request actor context
     * @param correlationId correlation ID for tracing
     * @param productId product identifier
     * @return product payload
     */
    public ProductPayload getProductById(
            ReviewRequestContext context,
            String correlationId,
            UUID productId
    ) {
        try {
            RemoteApiEnvelope<ProductPayload> envelope = restClient.get()
                    .uri("/api/v1/products/{productId}", productId)
                    .headers(headers -> {
                        if (context != null && context.hasSubject()) {
                            headers.set(HEADER_SUBJECT, context.subject());
                        }
                        if (context != null && context.authorizationHeader() != null) {
                            headers.set(HttpHeaders.AUTHORIZATION, context.authorizationHeader());
                        }
                        if (correlationId != null && !correlationId.isBlank()) {
                            headers.set(HEADER_CORRELATION, correlationId);
                        }
                        if (internalApiKey != null && !internalApiKey.isBlank()) {
                            headers.set(HEADER_INTERNAL_API_KEY, internalApiKey);
                        }
                    })
                    .retrieve()
                    .body(PRODUCT_RESPONSE_TYPE);
            if (envelope == null || !Boolean.TRUE.equals(envelope.success()) || envelope.data() == null) {
                throw new ReviewOperationException(
                        HttpStatus.BAD_GATEWAY,
                        "CATALOG_SERVICE_INVALID_RESPONSE",
                        "Catalog service returned an invalid product lookup response"
                );
            }
            return envelope.data();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new ReviewOperationException(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", "Product not found");
            }
            if (ex.getStatusCode() == HttpStatus.BAD_REQUEST) {
                throw new ReviewOperationException(
                        HttpStatus.BAD_REQUEST,
                        "PRODUCT_LOOKUP_INVALID",
                        "Catalog service rejected the product lookup request"
                );
            }
            log.warn("Catalog service get-product failed: status={} body={}",
                    ex.getStatusCode().value(), ex.getResponseBodyAsString());
            throw new ReviewOperationException(
                    HttpStatus.BAD_GATEWAY,
                    "CATALOG_SERVICE_ERROR",
                    "Product lookup is temporarily unavailable"
            );
        } catch (ResourceAccessException ex) {
            log.warn("Catalog service get-product unreachable: {}", ex.getMessage());
            throw new ReviewOperationException(
                    HttpStatus.BAD_GATEWAY,
                    "CATALOG_SERVICE_UNREACHABLE",
                    "Product lookup is temporarily unavailable"
            );
        }
    }

    /**
     * Minimal catalog product payload required by review-service.
     *
     * @param id product identifier
     * @param name product name
     * @param active whether product is active
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProductPayload(
            UUID id,
            String name,
            boolean active
    ) {
    }
}
