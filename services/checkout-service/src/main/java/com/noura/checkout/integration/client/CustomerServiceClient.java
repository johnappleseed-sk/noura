package com.noura.checkout.integration.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.noura.checkout.exception.CheckoutOperationException;
import com.noura.checkout.exception.NotFoundException;
import com.noura.checkout.integration.model.RemoteApiEnvelope;
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
 * REST adapter to customer-service account address APIs.
 */
@Slf4j
@Component
public class CustomerServiceClient {

    private static final String HEADER_SUBJECT = "X-Auth-Subject";
    private static final String HEADER_CORRELATION = "X-Correlation-ID";

    private static final ParameterizedTypeReference<RemoteApiEnvelope<AddressPayload>> ADDRESS_RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<RemoteApiEnvelope<CustomerLookupPayload>> CUSTOMER_LOOKUP_RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;
    private final String internalApiKey;

    /**
     * Creates customer-service REST adapter.
     *
     * @param builder rest client builder
     * @param baseUrl customer-service base URL
     */
    public CustomerServiceClient(
            RestClient.Builder builder,
            @Value("${services.customer.base-url:http://localhost:8089}") String baseUrl,
            @Value("${services.customer.internal-api-key:}") String internalApiKey
    ) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.internalApiKey = internalApiKey;
    }

    /**
     * Loads one address for current customer.
     *
     * @param customerRef resolved customer subject
     * @param addressId address identifier
     * @param authorizationHeader optional authorization header
     * @param correlationId correlation ID for tracing
     * @return address payload
     */
    public AddressPayload getAddress(
            String customerRef,
            UUID addressId,
            String authorizationHeader,
            String correlationId
    ) {
        try {
            RemoteApiEnvelope<AddressPayload> envelope = restClient.get()
                    .uri("/api/v1/account/addresses/{addressId}", addressId)
                    .headers(headers -> applyHeaders(headers, customerRef, authorizationHeader, correlationId))
                    .retrieve()
                    .body(ADDRESS_RESPONSE_TYPE);
            if (envelope == null || !Boolean.TRUE.equals(envelope.success()) || envelope.data() == null) {
                throw new CheckoutOperationException(
                        HttpStatus.BAD_GATEWAY,
                        "CUSTOMER_SERVICE_INVALID_RESPONSE",
                        "Customer service returned an invalid address response"
                );
            }
            return envelope.data();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new NotFoundException("ADDRESS_NOT_FOUND", "Address not found");
            }
            if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED || ex.getStatusCode() == HttpStatus.FORBIDDEN) {
                throw new CheckoutOperationException(
                        HttpStatus.UNAUTHORIZED,
                        "CUSTOMER_ADDRESS_ACCESS_DENIED",
                        "Address does not belong to current customer"
                );
            }
            log.warn("Customer service address lookup failed: status={} body={}",
                    ex.getStatusCode().value(), ex.getResponseBodyAsString());
            throw new CheckoutOperationException(
                    HttpStatus.BAD_GATEWAY,
                    "CUSTOMER_SERVICE_ERROR",
                    "Customer address data is temporarily unavailable"
            );
        } catch (ResourceAccessException ex) {
            log.warn("Customer service unreachable: {}", ex.getMessage());
            throw new CheckoutOperationException(
                    HttpStatus.BAD_GATEWAY,
                    "CUSTOMER_SERVICE_UNREACHABLE",
                    "Customer address data is temporarily unavailable"
            );
        }
    }

    /**
     * Resolves one customer profile by external subject for downstream internal notifications.
     *
     * @param externalSubject customer subject
     * @param correlationId correlation ID for tracing
     * @return internal customer lookup payload
     */
    public CustomerLookupPayload lookupByExternalSubject(String externalSubject, String correlationId) {
        try {
            RemoteApiEnvelope<CustomerLookupPayload> envelope = restClient.get()
                    .uri("/internal/customers/by-subject/{externalSubject}", externalSubject)
                    .headers(headers -> {
                        applyInternalHeaders(headers, correlationId);
                    })
                    .retrieve()
                    .body(CUSTOMER_LOOKUP_RESPONSE_TYPE);
            if (envelope == null || !Boolean.TRUE.equals(envelope.success()) || envelope.data() == null) {
                throw new CheckoutOperationException(
                        HttpStatus.BAD_GATEWAY,
                        "CUSTOMER_SERVICE_INVALID_RESPONSE",
                        "Customer service returned an invalid internal customer lookup response"
                );
            }
            return envelope.data();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new NotFoundException("CUSTOMER_NOT_FOUND", "Customer not found");
            }
            log.warn("Customer service subject lookup failed: status={} body={}",
                    ex.getStatusCode().value(), ex.getResponseBodyAsString());
            throw new CheckoutOperationException(
                    HttpStatus.BAD_GATEWAY,
                    "CUSTOMER_SERVICE_ERROR",
                    "Customer lookup is temporarily unavailable"
            );
        } catch (ResourceAccessException ex) {
            log.warn("Customer service internal lookup unreachable: {}", ex.getMessage());
            throw new CheckoutOperationException(
                    HttpStatus.BAD_GATEWAY,
                    "CUSTOMER_SERVICE_UNREACHABLE",
                    "Customer lookup is temporarily unavailable"
            );
        }
    }

    /**
     * Applies shared gateway-forwarded headers.
     *
     * @param headers mutable header collection
     * @param customerRef customer subject
     * @param authorizationHeader optional authorization header
     * @param correlationId correlation identifier
     */
    private void applyHeaders(
            HttpHeaders headers,
            String customerRef,
            String authorizationHeader,
            String correlationId
    ) {
        headers.set(HEADER_SUBJECT, customerRef);
        if (authorizationHeader != null && !authorizationHeader.isBlank()) {
            headers.set(HttpHeaders.AUTHORIZATION, authorizationHeader);
        }
        if (correlationId != null && !correlationId.isBlank()) {
            headers.set(HEADER_CORRELATION, correlationId);
        }
    }

    /**
     * Applies headers used by trusted internal customer lookups.
     *
     * @param headers mutable header collection
     * @param correlationId correlation identifier
     */
    private void applyInternalHeaders(HttpHeaders headers, String correlationId) {
        if (correlationId != null && !correlationId.isBlank()) {
            headers.set(HEADER_CORRELATION, correlationId);
        }
        if (internalApiKey != null && !internalApiKey.isBlank()) {
            headers.set("X-Internal-Api-Key", internalApiKey.trim());
        }
    }

    /**
     * Address payload returned by customer-service.
     *
     * @param id address identifier
     * @param fullName recipient full name
     * @param phone recipient phone
     * @param line1 address line 1
     * @param line2 address line 2
     * @param district district
     * @param city city
     * @param stateProvince state/province
     * @param postalCode postal code
     * @param countryCode country code
     * @param formattedAddress formatted address
     * @param validationStatus validation status
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AddressPayload(
            UUID id,
            String fullName,
            String phone,
            String line1,
            String line2,
            String district,
            String city,
            String stateProvince,
            String postalCode,
            String countryCode,
            String formattedAddress,
            String validationStatus
    ) {
    }

    /**
     * Internal customer lookup response payload.
     *
     * @param id customer identifier
     * @param externalSubject external subject
     * @param fullName customer full name
     * @param email customer email
     * @param phone customer phone
     * @param enabled enabled flag
     * @param defaultShippingAddressId default shipping address ID
     * @param defaultBillingAddressId default billing address ID
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CustomerLookupPayload(
            UUID id,
            String externalSubject,
            String fullName,
            String email,
            String phone,
            boolean enabled,
            UUID defaultShippingAddressId,
            UUID defaultBillingAddressId
    ) {
    }
}
