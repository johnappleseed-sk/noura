package com.noura.platform.testsupport.inventory.api;

import com.noura.platform.inventory.config.InventoryHeaderAuthenticationFilter;
import com.noura.platform.inventory.security.InventoryIdentityService;
import com.noura.platform.inventory.security.InventoryJwtAuthenticationFilter;
import com.noura.platform.inventory.security.InventorySecurityProperties;
import com.noura.platform.inventory.security.InventoryTokenService;
import com.noura.platform.inventory.security.InventoryUserPrincipal;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class InventoryWebMvcSecurityTestConfig {

    @Bean
    InventoryJwtAuthenticationFilter inventoryJwtAuthenticationFilter(InventorySecurityProperties securityProperties) {
        return new InventoryJwtAuthenticationFilter(
                new InventoryTokenService(securityProperties),
                new InventoryIdentityService(null, null, null) {
                    @Override
                    public InventoryUserPrincipal loadPrincipalByUserId(String userId) {
                        throw new UnsupportedOperationException("JWT authentication is not exercised in WebMvc controller tests");
                    }
                }
        );
    }

    @Bean
    InventoryHeaderAuthenticationFilter inventoryHeaderAuthenticationFilter(InventorySecurityProperties securityProperties) {
        return new InventoryHeaderAuthenticationFilter(securityProperties);
    }
}
