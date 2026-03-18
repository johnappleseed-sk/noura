package com.noura.customer.controller.support;

import com.noura.customer.exception.CustomerOperationException;
import com.noura.customer.service.model.CustomerIdentity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Unit tests for {@link CustomerIdentityResolver}.
 */
class CustomerIdentityResolverTest {

    private final CustomerIdentityResolver resolver = new CustomerIdentityResolver();

    /**
     * Verifies the resolver prioritizes forwarded gateway identity headers.
     */
    @Test
    void shouldResolveForwardedSubjectHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Auth-Subject", "subject-123");
        request.addHeader("X-Auth-Username", "customer@example.com");

        CustomerIdentity identity = resolver.resolveRequiredIdentity(request);

        Assertions.assertEquals("subject-123", identity.externalSubject());
        Assertions.assertEquals("customer@example.com", identity.emailHint());
    }

    /**
     * Verifies fallback identity derivation from a bearer token when forwarded subject is absent.
     */
    @Test
    void shouldResolveTokenFingerprintWhenBearerTokenExists() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer example-token-value");

        CustomerIdentity identity = resolver.resolveRequiredIdentity(request);

        Assertions.assertNotNull(identity.externalSubject());
        Assertions.assertTrue(identity.externalSubject().startsWith("token:"));
        Assertions.assertEquals(30, identity.externalSubject().length());
    }

    /**
     * Verifies unresolved identity fails with an authorization error.
     */
    @Test
    void shouldThrowUnauthorizedWhenNoIdentitySignalsExist() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        CustomerOperationException exception = Assertions.assertThrows(
                CustomerOperationException.class,
                () -> resolver.resolveRequiredIdentity(request)
        );

        Assertions.assertEquals("AUTH_SUBJECT_REQUIRED", exception.getCode());
        Assertions.assertEquals(401, exception.getStatus().value());
    }
}

