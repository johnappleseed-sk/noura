package com.noura.cart.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Ensures each request/response carries a correlation ID.
 */
@Component
public class RequestCorrelationFilter extends OncePerRequestFilter {

    /**
     * Correlation ID header name.
     */
    public static final String HEADER = "X-Correlation-ID";

    private static final String MDC_KEY = "correlationId";

    /**
     * Adds correlation metadata to MDC and response headers for the current request.
     *
     * @param request HTTP request
     * @param response HTTP response
     * @param filterChain downstream chain
     * @throws ServletException when downstream servlet processing fails
     * @throws IOException when downstream I/O fails
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String correlationId = resolveCorrelationId(request.getHeader(HEADER));
        MDC.put(MDC_KEY, correlationId);
        response.setHeader(HEADER, correlationId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    /**
     * Resolves a safe correlation ID value from the inbound header.
     *
     * @param headerValue inbound correlation header value
     * @return usable correlation ID
     */
    private String resolveCorrelationId(String headerValue) {
        if (headerValue == null || headerValue.isBlank()) {
            return UUID.randomUUID().toString();
        }
        String normalized = headerValue.trim();
        if (normalized.length() > 128) {
            return UUID.randomUUID().toString();
        }
        return normalized;
    }
}
