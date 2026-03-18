package com.noura.customer.controller;

import com.noura.customer.common.ApiResponse;
import com.noura.customer.config.InternalApiProperties;
import com.noura.customer.dto.internal.CustomerLookupResponse;
import com.noura.customer.exception.CustomerOperationException;
import com.noura.customer.service.CustomerAccountService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Internal customer lookup API for service-to-service integrations.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/customers")
public class InternalCustomerLookupController {

    private final CustomerAccountService customerAccountService;
    private final InternalApiProperties internalApiProperties;

    /**
     * Retrieves customer profile by profile ID.
     *
     * @param customerId customer profile identifier
     * @param providedApiKey internal API key provided by caller
     * @param request current HTTP request
     * @return customer lookup response envelope
     */
    @GetMapping("/{customerId}")
    public ApiResponse<CustomerLookupResponse> getById(
            @PathVariable UUID customerId,
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String providedApiKey,
            HttpServletRequest request
    ) {
        validateInternalApiKey(providedApiKey);
        CustomerLookupResponse result = customerAccountService.lookupById(customerId);
        return ApiResponse.ok("Customer lookup", result, request.getRequestURI());
    }

    /**
     * Retrieves customer profile by external subject key.
     *
     * @param externalSubject external identity subject key
     * @param providedApiKey internal API key provided by caller
     * @param request current HTTP request
     * @return customer lookup response envelope
     */
    @GetMapping("/by-subject/{externalSubject}")
    public ApiResponse<CustomerLookupResponse> getByExternalSubject(
            @PathVariable String externalSubject,
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String providedApiKey,
            HttpServletRequest request
    ) {
        validateInternalApiKey(providedApiKey);
        CustomerLookupResponse result = customerAccountService.lookupByExternalSubject(externalSubject);
        return ApiResponse.ok("Customer lookup", result, request.getRequestURI());
    }

    /**
     * Validates internal API key when one is configured.
     *
     * @param providedApiKey API key from request header
     * @throws CustomerOperationException when API key does not match configured value
     */
    private void validateInternalApiKey(String providedApiKey) {
        String configuredApiKey = trimToNull(internalApiProperties.getApiKey());
        if (configuredApiKey == null) {
            return;
        }
        if (!configuredApiKey.equals(trimToNull(providedApiKey))) {
            throw new CustomerOperationException(
                    HttpStatus.FORBIDDEN,
                    "INTERNAL_API_KEY_INVALID",
                    "Invalid internal API key"
            );
        }
    }

    /**
     * Trims source text and normalizes blanks to {@code null}.
     *
     * @param value source text
     * @return normalized text or {@code null}
     */
    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

