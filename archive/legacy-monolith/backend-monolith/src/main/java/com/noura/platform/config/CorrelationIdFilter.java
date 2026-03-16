package com.noura.platform.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String CORRELATION_ID_ATTRIBUTE = "correlationId";
    private static final String LEGACY_CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String MDC_KEY = "correlationId";
    private static final Pattern ALLOWED_CORRELATION_ID = Pattern.compile("^[A-Za-z0-9._-]{8,64}$");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String correlationId = resolveCorrelationId(firstNonBlank(
                request.getHeader(CORRELATION_ID_HEADER),
                request.getHeader(LEGACY_CORRELATION_ID_HEADER)
        ));

        MDC.put(MDC_KEY, correlationId);
        request.setAttribute(CORRELATION_ID_ATTRIBUTE, correlationId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);
        response.setHeader(LEGACY_CORRELATION_ID_HEADER, correlationId);

        try {
            filterChain.doFilter(new CorrelationIdRequestWrapper(request, correlationId), response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    private String resolveCorrelationId(String incoming) {
        if (incoming == null) {
            return UUID.randomUUID().toString();
        }
        String candidate = incoming.trim();
        if (!ALLOWED_CORRELATION_ID.matcher(candidate).matches()) {
            return UUID.randomUUID().toString();
        }
        return candidate;
    }

    private String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        if (fallback != null && !fallback.isBlank()) {
            return fallback;
        }
        return null;
    }

    private static final class CorrelationIdRequestWrapper extends HttpServletRequestWrapper {
        private final String correlationId;

        private CorrelationIdRequestWrapper(HttpServletRequest request, String correlationId) {
            super(request);
            this.correlationId = correlationId;
        }

        @Override
        public String getHeader(String name) {
            if (isCorrelationHeader(name)) {
                return correlationId;
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if (isCorrelationHeader(name)) {
                return Collections.enumeration(Collections.singletonList(correlationId));
            }
            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            Set<String> names = new LinkedHashSet<>(Collections.list(super.getHeaderNames()));
            names.add(CORRELATION_ID_HEADER);
            names.add(LEGACY_CORRELATION_ID_HEADER);
            return Collections.enumeration(names);
        }

        private boolean isCorrelationHeader(String name) {
            return CORRELATION_ID_HEADER.equalsIgnoreCase(name) || LEGACY_CORRELATION_ID_HEADER.equalsIgnoreCase(name);
        }
    }
}
