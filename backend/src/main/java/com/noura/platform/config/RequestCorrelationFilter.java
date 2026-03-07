package com.noura.platform.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class RequestCorrelationFilter extends OncePerRequestFilter {

    static final String CORRELATION_HEADER = "X-Correlation-ID";
    private static final String MDC_KEY = "correlationId";
    private static final int MAX_CORRELATION_ID_LENGTH = 64;
    private static final Pattern ALLOWED_CORRELATION_ID = Pattern.compile("^[A-Za-z0-9._-]{8,64}$");

    /**
     * Executes do filter internal.
     *
     * @param request The request payload for this operation.
     * @param response The response object used by this operation.
     * @param filterChain The filter criteria applied to this operation.
     * @throws ServletException If the operation cannot be completed.
     * @throws IOException If the operation cannot be completed.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String correlationId = resolveCorrelationId(request.getHeader(CORRELATION_HEADER));
        request.setAttribute(CORRELATION_HEADER, correlationId);
        response.setHeader(CORRELATION_HEADER, correlationId);
        MDC.put(MDC_KEY, correlationId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    private String resolveCorrelationId(String incomingHeader) {
        if (incomingHeader == null) {
            return UUID.randomUUID().toString();
        }
        String candidate = incomingHeader.trim();
        if (candidate.isEmpty()) {
            return UUID.randomUUID().toString();
        }
        if (candidate.length() > MAX_CORRELATION_ID_LENGTH) {
            return UUID.randomUUID().toString();
        }
        if (!ALLOWED_CORRELATION_ID.matcher(candidate).matches()) {
            return UUID.randomUUID().toString();
        }
        return candidate;
    }
}
