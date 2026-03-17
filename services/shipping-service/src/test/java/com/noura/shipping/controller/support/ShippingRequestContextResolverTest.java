package com.noura.shipping.controller.support;

import com.noura.shipping.config.InternalApiProperties;
import com.noura.shipping.service.model.ShippingRequestContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Unit tests for {@link ShippingRequestContextResolver}.
 */
class ShippingRequestContextResolverTest {

    /**
     * Verifies subject and role headers are resolved from gateway-forwarded values.
     */
    @Test
    void shouldResolveSubjectAndRoles() {
        InternalApiProperties properties = new InternalApiProperties();
        ShippingRequestContextResolver resolver = new ShippingRequestContextResolver(properties);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Auth-Subject", "customer-001");
        request.addHeader("X-Auth-Roles", "ADMIN, logistics");

        ShippingRequestContext context = resolver.resolve(request);

        Assertions.assertEquals("customer-001", context.subject());
        Assertions.assertTrue(context.hasRole("ADMIN"));
        Assertions.assertTrue(context.hasRole("LOGISTICS"));
    }

    /**
     * Verifies internal-call flag resolution when internal API key matches.
     */
    @Test
    void shouldResolveInternalCallWhenApiKeyMatches() {
        InternalApiProperties properties = new InternalApiProperties();
        properties.setApiKey("shared-secret");
        ShippingRequestContextResolver resolver = new ShippingRequestContextResolver(properties);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Internal-Api-Key", "shared-secret");

        ShippingRequestContext context = resolver.resolve(request);

        Assertions.assertTrue(context.internalCall());
    }

    /**
     * Verifies bearer tokens can provide a deterministic fallback subject.
     */
    @Test
    void shouldResolveSubjectFromBearerFallback() {
        InternalApiProperties properties = new InternalApiProperties();
        ShippingRequestContextResolver resolver = new ShippingRequestContextResolver(properties);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer sample-token");

        ShippingRequestContext context = resolver.resolve(request);

        Assertions.assertNotNull(context.subject());
        Assertions.assertTrue(context.subject().startsWith("bearer-"));
        Assertions.assertEquals("Bearer sample-token", context.authorizationHeader());
    }
}
