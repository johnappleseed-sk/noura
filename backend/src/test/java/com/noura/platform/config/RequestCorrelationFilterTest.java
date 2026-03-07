package com.noura.platform.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestCorrelationFilterTest {

    @Test
    void doFilterInternal_shouldGenerateCorrelationIdWhenMissing() throws Exception {
        RequestCorrelationFilter filter = new RequestCorrelationFilter();
        TrackingChain chain = new TrackingChain();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/products");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, chain);

        String correlationId = response.getHeader(RequestCorrelationFilter.CORRELATION_HEADER);
        assertFalse(correlationId == null || correlationId.isBlank());
        assertEquals(correlationId, chain.correlationIdSeenInChain);
        assertEquals(correlationId, request.getAttribute(RequestCorrelationFilter.CORRELATION_HEADER));
        assertNull(MDC.get("correlationId"));
        assertEquals(1, chain.invocations);
    }

    @Test
    void doFilterInternal_shouldReuseValidIncomingCorrelationId() throws Exception {
        RequestCorrelationFilter filter = new RequestCorrelationFilter();
        TrackingChain chain = new TrackingChain();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/orders");
        request.addHeader(RequestCorrelationFilter.CORRELATION_HEADER, "cid-orders-20260304");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, chain);

        assertEquals("cid-orders-20260304", response.getHeader(RequestCorrelationFilter.CORRELATION_HEADER));
        assertEquals("cid-orders-20260304", chain.correlationIdSeenInChain);
        assertEquals(1, chain.invocations);
    }

    @Test
    void doFilterInternal_shouldReplaceInvalidIncomingCorrelationId() throws Exception {
        RequestCorrelationFilter filter = new RequestCorrelationFilter();
        TrackingChain chain = new TrackingChain();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/cart");
        request.addHeader(RequestCorrelationFilter.CORRELATION_HEADER, "invalid cid with spaces");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, chain);

        String generated = response.getHeader(RequestCorrelationFilter.CORRELATION_HEADER);
        assertTrue(generated != null && generated.matches("^[a-f0-9\\-]{36}$"));
        assertEquals(generated, chain.correlationIdSeenInChain);
        assertEquals(1, chain.invocations);
    }

    private static final class TrackingChain implements FilterChain {
        private int invocations;
        private String correlationIdSeenInChain;

        @Override
        public void doFilter(ServletRequest request, ServletResponse response) {
            invocations++;
            correlationIdSeenInChain = MDC.get("correlationId");
        }
    }
}
