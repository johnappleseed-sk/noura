package com.noura.promotion.controller.support;

import com.noura.promotion.config.InternalApiProperties;
import com.noura.promotion.service.model.PromotionRequestContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Unit tests for {@link PromotionRequestContextResolver}.
 */
class PromotionRequestContextResolverTest {

    /**
     * Verifies subject and role headers are resolved from gateway-forwarded values.
     */
    @Test
    void shouldResolveSubjectAndRoles() {
        InternalApiProperties properties = new InternalApiProperties();
        PromotionRequestContextResolver resolver = new PromotionRequestContextResolver(properties);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Auth-Subject", "marketing-user-001");
        request.addHeader("X-Auth-Roles", "ADMIN, marketing_manager");

        PromotionRequestContext context = resolver.resolve(request);

        Assertions.assertEquals("marketing-user-001", context.subject());
        Assertions.assertTrue(context.hasRole("ADMIN"));
        Assertions.assertTrue(context.hasRole("MARKETING_MANAGER"));
    }

    /**
     * Verifies internal-call flag resolution when internal API key matches.
     */
    @Test
    void shouldResolveInternalCallWhenApiKeyMatches() {
        InternalApiProperties properties = new InternalApiProperties();
        properties.setApiKey("shared-secret");
        PromotionRequestContextResolver resolver = new PromotionRequestContextResolver(properties);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Internal-Api-Key", "shared-secret");

        PromotionRequestContext context = resolver.resolve(request);

        Assertions.assertTrue(context.internalCall());
    }

    /**
     * Verifies bearer tokens can provide a deterministic fallback subject.
     */
    @Test
    void shouldResolveSubjectFromBearerFallback() {
        InternalApiProperties properties = new InternalApiProperties();
        PromotionRequestContextResolver resolver = new PromotionRequestContextResolver(properties);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer sample-token");

        PromotionRequestContext context = resolver.resolve(request);

        Assertions.assertNotNull(context.subject());
        Assertions.assertTrue(context.subject().startsWith("bearer-"));
        Assertions.assertEquals("Bearer sample-token", context.authorizationHeader());
    }
}
