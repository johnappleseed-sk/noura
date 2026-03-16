package com.noura.platform.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ApiRequestLoggingFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(ApiRequestLoggingFilter.class);
    private static final long WARN_MS_THRESHOLD = 1_500L;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        long startedAt = System.nanoTime();
        String principal = currentPrincipal();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = (System.nanoTime() - startedAt) / 1_000_000L;
            String correlationId = MDC.get("correlationId");
            int status = response.getStatus();
            Logger logger = logByStatus(status);
            String method = request.getMethod();
            String path = request.getRequestURI();
            String query = request.getQueryString();
            String clientIp = request.getRemoteAddr();

            logger.info(
                    "method={} path={} query={} status={} durationMs={} correlationId={} user={} clientIp={}",
                    method,
                    path,
                    query,
                    status,
                    durationMs,
                    correlationId,
                    principal,
                    clientIp
            );

            if (durationMs >= WARN_MS_THRESHOLD) {
                logger.warn(
                        "slow_api_request method={} path={} query={} status={} durationMs={} thresholdMs={} correlationId={} user={} clientIp={}",
                        method,
                        path,
                        query,
                        status,
                        durationMs,
                        WARN_MS_THRESHOLD,
                        correlationId,
                        principal,
                        clientIp
                );
            }
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        if (requestUri == null || requestUri.isBlank()) {
            return true;
        }
        return requestUri.startsWith("/actuator/")
                || requestUri.startsWith("/swagger-ui")
                || requestUri.startsWith("/v3/api-docs")
                || requestUri.equals("/favicon.ico")
                || requestUri.startsWith("/uploads");
    }

    private Logger logByStatus(int status) {
        if (status >= 500) {
            return log;
        }
        if (status >= 400) {
            return log;
        }
        return log;
    }

    private String currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return "anonymous";
        }
        String name = authentication.getName();
        return name == null || name.isBlank() ? "anonymous" : name;
    }
}
