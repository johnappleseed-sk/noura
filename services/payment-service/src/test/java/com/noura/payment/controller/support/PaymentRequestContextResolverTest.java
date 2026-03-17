package com.noura.payment.controller.support;

import com.noura.payment.config.InternalApiProperties;
import com.noura.payment.service.model.PaymentRequestContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Unit tests for {@link PaymentRequestContextResolver}.
 */
class PaymentRequestContextResolverTest {

    /**
     * Verifies subject and role headers are resolved from gateway-forwarded values.
     */
    @Test
    void shouldResolveSubjectAndRoles() {
        InternalApiProperties properties = new InternalApiProperties();
        PaymentRequestContextResolver resolver = new PaymentRequestContextResolver(properties);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Auth-Subject", "customer-001");
        request.addHeader("X-Auth-Roles", "ADMIN, finance_officer");

        PaymentRequestContext context = resolver.resolve(request);

        Assertions.assertEquals("customer-001", context.subject());
        Assertions.assertTrue(context.hasRole("ADMIN"));
        Assertions.assertTrue(context.hasRole("FINANCE_OFFICER"));
    }

    /**
     * Verifies internal-call flag resolution when internal API key matches.
     */
    @Test
    void shouldResolveInternalCallWhenApiKeyMatches() {
        InternalApiProperties properties = new InternalApiProperties();
        properties.setApiKey("shared-secret");
        PaymentRequestContextResolver resolver = new PaymentRequestContextResolver(properties);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Internal-Api-Key", "shared-secret");

        PaymentRequestContext context = resolver.resolve(request);

        Assertions.assertTrue(context.internalCall());
    }

    /**
     * Verifies bearer tokens can provide a deterministic fallback subject.
     */
    @Test
    void shouldResolveSubjectFromBearerFallback() {
        InternalApiProperties properties = new InternalApiProperties();
        PaymentRequestContextResolver resolver = new PaymentRequestContextResolver(properties);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer sample-token");

        PaymentRequestContext context = resolver.resolve(request);

        Assertions.assertNotNull(context.subject());
        Assertions.assertTrue(context.subject().startsWith("bearer-"));
        Assertions.assertEquals("Bearer sample-token", context.authorizationHeader());
    }
}
