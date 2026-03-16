package com.noura.platform.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RateLimitFilterTest {

    @Test
    void doFilterInternal_shouldUseForwardedHeaderWhenProxyIsTrusted() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(configuredProperties(true, "10.0.0.1"));
        TrackingChain chain = new TrackingChain();

        MockHttpServletRequest first = request("10.0.0.1", "203.0.113.10, 10.0.0.1", null);
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        filter.doFilterInternal(first, firstResponse, chain);

        MockHttpServletRequest second = request("10.0.0.1", "203.0.113.11, 10.0.0.1", null);
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        filter.doFilterInternal(second, secondResponse, chain);

        assertEquals(200, firstResponse.getStatus());
        assertEquals(200, secondResponse.getStatus());
        assertEquals(2, chain.invocations);
    }

    @Test
    void doFilterInternal_shouldIgnoreForwardedHeaderWhenProxyIsUntrusted() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(configuredProperties(true, "10.0.0.1"));
        TrackingChain chain = new TrackingChain();

        MockHttpServletRequest first = request("10.0.0.9", "203.0.113.10, 10.0.0.9", null);
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        filter.doFilterInternal(first, firstResponse, chain);

        MockHttpServletRequest second = request("10.0.0.9", "203.0.113.11, 10.0.0.9", null);
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        filter.doFilterInternal(second, secondResponse, chain);

        assertEquals(200, firstResponse.getStatus());
        assertEquals(429, secondResponse.getStatus());
        assertEquals(1, chain.invocations);
    }

    @Test
    void doFilterInternal_shouldIgnoreForwardedHeaderWhenTrustDisabled() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(configuredProperties(false, "10.0.0.1"));
        TrackingChain chain = new TrackingChain();

        MockHttpServletRequest first = request("10.0.0.1", "203.0.113.10, 10.0.0.1", null);
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        filter.doFilterInternal(first, firstResponse, chain);

        MockHttpServletRequest second = request("10.0.0.1", "203.0.113.11, 10.0.0.1", null);
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        filter.doFilterInternal(second, secondResponse, chain);

        assertEquals(200, firstResponse.getStatus());
        assertEquals(429, secondResponse.getStatus());
        assertEquals(1, chain.invocations);
    }

    @Test
    void doFilterInternal_shouldUseXRealIpWhenTrustedAndForwardedMissing() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(configuredProperties(true, "10.0.0.1"));
        TrackingChain chain = new TrackingChain();

        MockHttpServletRequest first = request("10.0.0.1", null, "198.51.100.20");
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        filter.doFilterInternal(first, firstResponse, chain);

        MockHttpServletRequest second = request("10.0.0.1", null, "198.51.100.21");
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        filter.doFilterInternal(second, secondResponse, chain);

        assertEquals(200, firstResponse.getStatus());
        assertEquals(200, secondResponse.getStatus());
        assertEquals(2, chain.invocations);
    }

    private AppProperties configuredProperties(boolean trustForwardedHeaders, String trustedProxies) {
        AppProperties properties = new AppProperties();
        AppProperties.RateLimit limit = properties.getRateLimit();
        limit.setCapacity(1);
        limit.setRefillTokens(1);
        limit.setRefillMinutes(60);
        limit.setKeyTtlMinutes(30);
        limit.setMaxKeys(1000);
        limit.setTrustForwardedHeaders(trustForwardedHeaders);
        limit.setForwardedIpHeader("X-Forwarded-For");
        limit.setTrustedProxyAddresses(trustedProxies);
        return properties;
    }

    private MockHttpServletRequest request(String remoteAddr, String forwardedFor, String realIp) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/products");
        request.setRemoteAddr(remoteAddr);
        if (forwardedFor != null) {
            request.addHeader("X-Forwarded-For", forwardedFor);
        }
        if (realIp != null) {
            request.addHeader("X-Real-IP", realIp);
        }
        return request;
    }

    private static final class TrackingChain implements FilterChain {
        private int invocations;

        @Override
        public void doFilter(ServletRequest request, ServletResponse response) {
            invocations++;
        }
    }
}
