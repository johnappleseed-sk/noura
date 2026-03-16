package com.noura.platform.config;

/**
 * Backward-compatible alias for tests and legacy imports migrated to {@link CorrelationIdFilter}.
 */
@Deprecated(since = "2026-03", forRemoval = true)
public class RequestCorrelationFilter extends CorrelationIdFilter {

    public static final String CORRELATION_HEADER = CORRELATION_ID_HEADER;
}
