package com.noura.pricing.integration.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.noura.pricing.exception.NotFoundException;
import com.noura.pricing.exception.PricingOperationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.UUID;

/**
 * Internal catalog lookup client used to translate legacy variant IDs into product IDs.
 */
@Slf4j
@Component
public class CatalogVariantLookupClient {

    private static final ParameterizedTypeReference<RemoteEnvelope<VariantLookupPayload>> RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;

    /**
     * Creates catalog lookup client.
     *
     * @param builder rest client builder
     * @param baseUrl catalog-service base URL
     */
    public CatalogVariantLookupClient(
            RestClient.Builder builder,
            @Value("${services.catalog.base-url:http://localhost:8084}") String baseUrl
    ) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    /**
     * Resolves a variant identifier into its owning product identifier.
     *
     * @param variantId variant identifier
     * @return resolved product identifier
     */
    public UUID resolveProductId(UUID variantId) {
        try {
            RemoteEnvelope<VariantLookupPayload> envelope = restClient.get()
                    .uri("/internal/catalog/variants/{variantId}", variantId)
                    .retrieve()
                    .body(RESPONSE_TYPE);
            if (envelope == null || !Boolean.TRUE.equals(envelope.success()) || envelope.data() == null) {
                throw new PricingOperationException(
                        HttpStatus.BAD_GATEWAY,
                        "CATALOG_VARIANT_LOOKUP_INVALID",
                        "Catalog variant lookup returned an invalid response"
                );
            }
            return envelope.data().productId();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new NotFoundException("VARIANT_NOT_FOUND", "Variant not found");
            }
            log.warn("Catalog variant lookup failed: status={} body={}", ex.getStatusCode().value(), ex.getResponseBodyAsString());
            throw new PricingOperationException(
                    HttpStatus.BAD_GATEWAY,
                    "CATALOG_VARIANT_LOOKUP_FAILED",
                    "Catalog variant lookup is temporarily unavailable"
            );
        } catch (ResourceAccessException ex) {
            log.warn("Catalog variant lookup unreachable: {}", ex.getMessage());
            throw new PricingOperationException(
                    HttpStatus.BAD_GATEWAY,
                    "CATALOG_VARIANT_LOOKUP_UNREACHABLE",
                    "Catalog variant lookup is temporarily unavailable"
            );
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RemoteEnvelope<T>(
            Boolean success,
            T data
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record VariantLookupPayload(
            UUID variantId,
            UUID productId,
            String sku,
            String variantName,
            boolean active
    ) {
    }
}
