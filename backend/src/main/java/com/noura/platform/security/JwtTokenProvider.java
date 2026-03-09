package com.noura.platform.security;

import com.noura.platform.config.AppProperties;
import com.noura.platform.domain.enums.RoleType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final AppProperties appProperties;

    /**
     * Generates access token.
     *
     * @param userId The user id used to locate the target record.
     * @param email The email value.
     * @param roles The roles value.
     * @return The result of generate access token.
     */
    public String generateAccessToken(UUID userId, String email, Set<RoleType> roles) {
        Instant now = Instant.now();
        Instant expiry = now.plus(appProperties.getJwt().getAccessTokenValidityMinutes(), ChronoUnit.MINUTES);
        return Jwts.builder()
                .subject(email)
                .issuer(appProperties.getJwt().getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .claim("uid", userId.toString())
                .claim("roles", roles.stream().map(Enum::name).toList())
                .signWith(secretKey())
                .compact();
    }

    /**
     * Executes extract all claims.
     *
     * @param token The token value.
     * @return The result of extract all claims.
     */
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Validates validate token.
     *
     * @param token The token value.
     * @return The result of validate token.
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * Executes extract email.
     *
     * @param token The token value.
     * @return The result of extract email.
     */
    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Executes secret key.
     *
     * @return The result of secret key.
     */
    private SecretKey secretKey() {
        String configuredSecret = appProperties.getJwt().getSecret();
        if (configuredSecret == null || configuredSecret.isBlank()) {
            throw new IllegalStateException("JWT secret must be configured");
        }
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(configuredSecret);
        } catch (RuntimeException ignored) {
            keyBytes = configuredSecret.getBytes(StandardCharsets.UTF_8);
        }
        if (keyBytes.length < 32) {
            keyBytes = sha256(configuredSecret);
        /**
         * Executes if.
         *
         * @param param1 The param1 value.
         * @return The result of if.
         */
        } else if (keyBytes.length > 64) {
            keyBytes = Arrays.copyOf(keyBytes, 64);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Executes sha256.
     *
     * @param value The value value.
     * @return A list of matching items.
     */
    private byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", ex);
        }
    }
}
