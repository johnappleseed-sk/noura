package com.noura.platform.inventory.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.Optional;

@Configuration
public class InventoryAuditingConfig {

    @Bean
    public AuditorAware<String> inventoryAuditorAware() {
        return () -> Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getName)
                .filter(name -> !name.isBlank())
                .or(() -> Optional.of("inventory-system"));
    }

    @Bean
    public DateTimeProvider inventoryDateTimeProvider() {
        return () -> Optional.of(Instant.now());
    }
}
