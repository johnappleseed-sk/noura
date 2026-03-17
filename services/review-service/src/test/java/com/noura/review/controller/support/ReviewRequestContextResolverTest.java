package com.noura.review.controller.support;

import com.noura.review.config.InternalApiProperties;
import com.noura.review.service.model.ReviewRequestContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Unit tests for {@link ReviewRequestContextResolver}.
 */
class ReviewRequestContextResolverTest {

    /**
     * Verifies subject, username, and roles are resolved from gateway-forwarded headers.
     */
    @Test
    void shouldResolveSubjectUsernameAndRoles() {
        InternalApiProperties properties = new InternalApiProperties();
        ReviewRequestContextResolver resolver = new ReviewRequestContextResolver(properties);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Auth-Subject", "customer-123");
        request.addHeader("X-Auth-Username", "Noura Shopper");
        request.addHeader("X-Auth-Roles", "customer, ROLE_MODERATOR");

        ReviewRequestContext context = resolver.resolve(request);

        Assertions.assertEquals("customer-123", context.subject());
        Assertions.assertEquals("Noura Shopper", context.username());
        Assertions.assertTrue(context.hasRole("CUSTOMER"));
        Assertions.assertTrue(context.hasRole("ROLE_MODERATOR"));
    }

    /**
     * Verifies internal-call flag resolution when API keys match.
     */
    @Test
    void shouldResolveInternalCallWhenApiKeyMatches() {
        InternalApiProperties properties = new InternalApiProperties();
        properties.setApiKey("shared-secret");
        ReviewRequestContextResolver resolver = new ReviewRequestContextResolver(properties);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Internal-Api-Key", "shared-secret");

        ReviewRequestContext context = resolver.resolve(request);

        Assertions.assertTrue(context.internalCall());
    }

    /**
     * Verifies bearer token fallback creates a deterministic subject when the gateway did not forward one.
     */
    @Test
    void shouldResolveSubjectFromBearerFallback() {
        InternalApiProperties properties = new InternalApiProperties();
        ReviewRequestContextResolver resolver = new ReviewRequestContextResolver(properties);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer sample-token");

        ReviewRequestContext context = resolver.resolve(request);

        Assertions.assertNotNull(context.subject());
        Assertions.assertTrue(context.subject().startsWith("bearer-"));
        Assertions.assertEquals("Bearer sample-token", context.authorizationHeader());
    }

    /**
     * Verifies the resolver prefers the first forwarded IP address.
     */
    @Test
    void shouldPreferFirstForwardedIpAddress() {
        InternalApiProperties properties = new InternalApiProperties();
        ReviewRequestContextResolver resolver = new ReviewRequestContextResolver(properties);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.10, 10.0.0.4");
        request.setRemoteAddr("127.0.0.1");

        ReviewRequestContext context = resolver.resolve(request);

        Assertions.assertEquals("203.0.113.10", context.remoteAddress());
    }
}
