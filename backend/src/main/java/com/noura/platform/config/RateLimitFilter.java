package com.noura.platform.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final long CLEANUP_INTERVAL_REQUESTS = 100;

    private final AppProperties appProperties;
    private final Map<String, BucketEntry> buckets = new ConcurrentHashMap<>();
    private final AtomicLong requestCounter = new AtomicLong();

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
        Instant now = Instant.now();
        cleanupIfNeeded(now);

        String key = resolveClientKey(request);
        BucketEntry entry = getOrCreateBucketEntry(key, now);
        if (entry == null || !entry.bucket().tryConsume(1)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"message\":\"Rate limit exceeded\"}");
            return;
        }

        entry.touch(now);
        filterChain.doFilter(request, response);
    }

    private BucketEntry getOrCreateBucketEntry(String key, Instant now) {
        BucketEntry existing = buckets.get(key);
        if (existing != null) {
            return existing;
        }

        if (buckets.size() >= appProperties.getRateLimit().getMaxKeys()) {
            evictExpiredBuckets(now);
            if (buckets.size() >= appProperties.getRateLimit().getMaxKeys()) {
                return null;
            }
        }

        BucketEntry created = new BucketEntry(newBucket(key), now);
        BucketEntry previous = buckets.putIfAbsent(key, created);
        return previous == null ? created : previous;
    }

    private void cleanupIfNeeded(Instant now) {
        if (requestCounter.incrementAndGet() % CLEANUP_INTERVAL_REQUESTS != 0) {
            return;
        }
        evictExpiredBuckets(now);
    }

    private void evictExpiredBuckets(Instant now) {
        Duration ttl = Duration.ofMinutes(Math.max(1, appProperties.getRateLimit().getKeyTtlMinutes()));
        buckets.entrySet().removeIf(entry -> Duration.between(entry.getValue().lastSeenAt(), now).compareTo(ttl) > 0);
    }

    private String resolveClientKey(HttpServletRequest request) {
        AppProperties.RateLimit limit = appProperties.getRateLimit();
        String remoteAddress = normalizeAddress(request.getRemoteAddr());

        if (limit.isTrustForwardedHeaders() && isTrustedProxy(remoteAddress, limit.getTrustedProxyAddresses())) {
            String forwarded = extractIp(request.getHeader(limit.getForwardedIpHeader()));
            if (!forwarded.isBlank()) {
                return forwarded;
            }
            String realIp = extractIp(request.getHeader("X-Real-IP"));
            if (!realIp.isBlank()) {
                return realIp;
            }
        }

        return remoteAddress == null || remoteAddress.isBlank() ? "unknown" : remoteAddress;
    }

    private boolean isTrustedProxy(String remoteAddress, String trustedProxyAddresses) {
        if (remoteAddress == null || remoteAddress.isBlank()) {
            return false;
        }
        Set<String> trusted = parseTrustedProxies(trustedProxyAddresses);
        if (trusted.isEmpty()) {
            return false;
        }
        return trusted.contains(remoteAddress);
    }

    private Set<String> parseTrustedProxies(String trustedProxyAddresses) {
        if (trustedProxyAddresses == null || trustedProxyAddresses.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(trustedProxyAddresses.split(","))
                .map(this::normalizeAddress)
                .filter(address -> address != null && !address.isBlank())
                .collect(Collectors.toSet());
    }

    private String normalizeAddress(String address) {
        if (address == null) {
            return null;
        }
        String normalized = address.trim();
        if (normalized.startsWith("[") && normalized.endsWith("]") && normalized.length() > 2) {
            return normalized.substring(1, normalized.length() - 1);
        }
        return normalized;
    }

    private String extractIp(String headerValue) {
        if (headerValue == null || headerValue.isBlank()) {
            return "";
        }
        return normalizeAddress(headerValue.split(",")[0]);
    }

    /**
     * Executes new bucket.
     *
     * @param ignored The ignored value.
     * @return The result of new bucket.
     */
    private Bucket newBucket(String ignored) {
        AppProperties.RateLimit limit = appProperties.getRateLimit();
        Bandwidth bandwidth = Bandwidth.classic(
                limit.getCapacity(),
                Refill.intervally(limit.getRefillTokens(), Duration.ofMinutes(limit.getRefillMinutes()))
        );
        return Bucket.builder().addLimit(bandwidth).build();
    }

    private static final class BucketEntry {
        private final Bucket bucket;
        private volatile Instant lastSeenAt;

        private BucketEntry(Bucket bucket, Instant lastSeenAt) {
            this.bucket = bucket;
            this.lastSeenAt = lastSeenAt;
        }

        private Bucket bucket() {
            return bucket;
        }

        private Instant lastSeenAt() {
            return lastSeenAt;
        }

        private void touch(Instant instant) {
            this.lastSeenAt = instant;
        }
    }
}
