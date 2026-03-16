package com.noura.platform.security;

import com.noura.platform.config.AppProperties;
import com.noura.platform.domain.enums.RoleType;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenProviderInvalidSignatureTest {

    @Test
    void validateToken_shouldRejectTokenSignedWithDifferentSecret() {
        JwtTokenProvider issuerProvider = new JwtTokenProvider(propertiesWithSecret("0123456789abcdef0123456789abcdef"));
        JwtTokenProvider verifierProvider = new JwtTokenProvider(propertiesWithSecret("abcdef0123456789abcdef0123456789"));

        String token = issuerProvider.generateAccessToken(
                UUID.randomUUID(),
                "customer@noura.test",
                Set.of(RoleType.CUSTOMER)
        );

        assertTrue(issuerProvider.validateToken(token));
        assertFalse(verifierProvider.validateToken(token));
    }

    @Test
    void validateToken_shouldRejectTamperedToken() {
        JwtTokenProvider provider = new JwtTokenProvider(propertiesWithSecret("0123456789abcdef0123456789abcdef"));
        String token = provider.generateAccessToken(
                UUID.randomUUID(),
                "customer@noura.test",
                Set.of(RoleType.CUSTOMER)
        );

        String[] parts = token.split("\\.");
        String payload = parts[1];
        String tamperedPayload = payload.substring(0, payload.length() - 1)
                + (payload.endsWith("a") ? "b" : "a");
        String tampered = parts[0] + "." + tamperedPayload + "." + parts[2];
        assertFalse(provider.validateToken(tampered));
    }

    private AppProperties propertiesWithSecret(String secret) {
        AppProperties properties = new AppProperties();
        properties.getJwt().setSecret(secret);
        properties.getJwt().setIssuer("noura-test");
        properties.getJwt().setAccessTokenValidityMinutes(30);
        return properties;
    }
}
