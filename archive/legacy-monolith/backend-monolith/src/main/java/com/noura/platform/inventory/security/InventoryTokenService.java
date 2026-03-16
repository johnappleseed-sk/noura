package com.noura.platform.inventory.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class InventoryTokenService {

    private static final String CLAIM_TYPE = "type";
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_FULL_NAME = "fullName";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_PERMISSIONS = "permissions";
    private static final String TYPE_ACCESS = "INVENTORY_ACCESS";

    private final InventorySecurityProperties securityProperties;

    public InventoryTokenService(InventorySecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    public InventoryTokenPayload issueAccessToken(InventoryUserPrincipal principal) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(normalizeMinutes(securityProperties.getJwt().getAccessTokenMinutes()), ChronoUnit.MINUTES);
        String token = Jwts.builder()
                .subject(principal.userId())
                .claim(CLAIM_TYPE, TYPE_ACCESS)
                .claim("username", principal.username())
                .claim(CLAIM_EMAIL, principal.email())
                .claim(CLAIM_FULL_NAME, principal.fullName())
                .claim(CLAIM_ROLES, principal.roles())
                .claim(CLAIM_PERMISSIONS, principal.permissions())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey())
                .compact();
        return new InventoryTokenPayload(token, expiresAt);
    }

    public Claims parseAccessToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey())
                .clockSkewSeconds(normalizedClockSkewSeconds(securityProperties.getJwt().getAllowedClockSkewSeconds()))
                .build()
                .parseSignedClaims(token.trim())
                .getPayload();
        String type = claims.get(CLAIM_TYPE, String.class);
        if (!TYPE_ACCESS.equals(type)) {
            throw new IllegalArgumentException("Unexpected inventory token type");
        }
        return claims;
    }

    public record InventoryTokenPayload(String token, Instant expiresAt) {
    }

    private SecretKey secretKey() {
        String secret = securityProperties.getJwt().getSecret();
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException("Inventory JWT secret is not configured");
        }
        byte[] keyBytes = secret.trim().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("Inventory JWT secret must be at least 32 bytes");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private long normalizeMinutes(long configured) {
        return configured <= 0 ? 60 : configured;
    }

    private long normalizedClockSkewSeconds(long configured) {
        if (configured < 0) {
            return 0;
        }
        return Math.min(configured, 300);
    }
}
