package com.noura.checkout.controller.support;

import com.noura.checkout.config.RequestCorrelationFilter;
import com.noura.checkout.service.model.CheckoutRequestContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Unit tests for {@link CheckoutRequestContextResolver}.
 */
class CheckoutRequestContextResolverTest {

    /**
     * Verifies subject, roles, and correlation headers are resolved from forwarded values.
     */
    @Test
    void shouldResolveSubjectRolesAndCorrelation() {
        CheckoutRequestContextResolver resolver = new CheckoutRequestContextResolver();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Auth-Subject", "customer-001");
        request.addHeader("X-Auth-Roles", "CUSTOMER, role_user");
        request.addHeader(RequestCorrelationFilter.HEADER, "corr-123");

        CheckoutRequestContext context = resolver.resolve(request);

        Assertions.assertEquals("customer-001", context.subject());
        Assertions.assertTrue(context.hasRole("CUSTOMER"));
        Assertions.assertEquals("corr-123", context.correlationId());
    }

    /**
     * Verifies bearer token fallback produces deterministic subject when forwarded subject is absent.
     */
    @Test
    void shouldResolveSubjectFromBearerFallback() {
        CheckoutRequestContextResolver resolver = new CheckoutRequestContextResolver();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer sample-token");

        CheckoutRequestContext context = resolver.resolve(request);

        Assertions.assertNotNull(context.subject());
        Assertions.assertTrue(context.subject().startsWith("bearer-"));
    }
}

