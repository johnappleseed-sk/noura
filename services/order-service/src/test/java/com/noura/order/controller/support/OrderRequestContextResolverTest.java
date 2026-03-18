package com.noura.order.controller.support;

import com.noura.order.config.InternalApiProperties;
import com.noura.order.service.model.OrderRequestContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Unit tests for {@link OrderRequestContextResolver}.
 */
class OrderRequestContextResolverTest {

    /**
     * Verifies subject and role headers are resolved from gateway-forwarded values.
     */
    @Test
    void shouldResolveSubjectAndRoles() {
        InternalApiProperties properties = new InternalApiProperties();
        OrderRequestContextResolver resolver = new OrderRequestContextResolver(properties);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Auth-Subject", "customer-001");
        request.addHeader("X-Auth-Roles", "ADMIN, role_order_manager");

        OrderRequestContext context = resolver.resolve(request);

        Assertions.assertEquals("customer-001", context.subject());
        Assertions.assertTrue(context.hasRole("ADMIN"));
        Assertions.assertTrue(context.hasRole("ROLE_ORDER_MANAGER"));
    }

    /**
     * Verifies internal-call flag resolution when internal API key matches.
     */
    @Test
    void shouldResolveInternalCallWhenApiKeyMatches() {
        InternalApiProperties properties = new InternalApiProperties();
        properties.setApiKey("shared-secret");
        OrderRequestContextResolver resolver = new OrderRequestContextResolver(properties);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Internal-Api-Key", "shared-secret");

        OrderRequestContext context = resolver.resolve(request);

        Assertions.assertTrue(context.internalCall());
    }

    /**
     * Verifies bearer tokens can provide a deterministic fallback subject.
     */
    @Test
    void shouldResolveSubjectFromBearerFallback() {
        InternalApiProperties properties = new InternalApiProperties();
        OrderRequestContextResolver resolver = new OrderRequestContextResolver(properties);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer sample-token");

        OrderRequestContext context = resolver.resolve(request);

        Assertions.assertNotNull(context.subject());
        Assertions.assertTrue(context.subject().startsWith("bearer-"));
    }
}

