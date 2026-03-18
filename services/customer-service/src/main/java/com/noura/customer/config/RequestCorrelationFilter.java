package com.noura.customer.config;

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
 * Propagates correlation IDs through request lifecycle and MDC.
 */
@Component
public class RequestCorrelationFilter extends OncePerRequestFilter {

    /**
     * Correlation header key.
     */
    public static final String HEADER = "X-Correlation-ID";

    private static final String MDC_KEY = "correlationId";

    /**
     * Resolves and stores correlation ID for each request.
     *
     * @param request HTTP request
     * @param response HTTP response
     * @param filterChain downstream filter chain
     * @throws ServletException when downstream processing fails
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
     * Resolves safe correlation ID from incoming header value.
     *
     * @param headerValue incoming correlation header
     * @return resolved correlation ID
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
