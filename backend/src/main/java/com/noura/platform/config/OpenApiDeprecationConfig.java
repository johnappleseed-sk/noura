package com.noura.platform.config;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiDeprecationConfig {

    private static final String EXTENSION_CONTRACT_STATUS = "x-contract-status";
    private static final String EXTENSION_DEPRECATION_REASON = "x-deprecation-reason";

    private static final String STATUS_TRANSITIONAL = "transitional";
    private static final String STATUS_DEPRECATED = "deprecated";

    private static final String INVENTORY_ALIAS_REASON =
            "Transitional compatibility aliases. Use /api/inventory/v1/* endpoints.";
    private static final String DUPLICATE_STOCK_ALIAS_REASON =
            "Duplicate stock aliases. Keep one canonical inventory path under /api/inventory/v1/*.";
    private static final String LEGACY_STOREFRONT_REASON =
            "Legacy storefront profile endpoints. Migrate consumers to /api/v1/*.";
    private static final String LEGACY_COMMERCE_REASON =
            "Legacy commerce APIs are non-canonical for active runtime.";

    @Bean
    public OpenApiCustomizer contractDeprecationCustomizer() {
        return openApi -> {
            if (openApi == null || openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().forEach(this::applyContractDeprecation);
        };
    }

    private void applyContractDeprecation(String path, PathItem pathItem) {
        if (pathItem == null) {
            return;
        }
        if (isDuplicateStockAlias(path)) {
            markDeprecated(pathItem, STATUS_TRANSITIONAL, DUPLICATE_STOCK_ALIAS_REASON);
            return;
        }
        if (path.startsWith("/api/v1/inventory")) {
            markDeprecated(pathItem, STATUS_TRANSITIONAL, INVENTORY_ALIAS_REASON);
            return;
        }
        if (path.startsWith("/api/storefront/v1")) {
            markDeprecated(pathItem, STATUS_DEPRECATED, LEGACY_STOREFRONT_REASON);
            return;
        }
        if (path.startsWith("/commerce/api/v1")) {
            markDeprecated(pathItem, STATUS_DEPRECATED, LEGACY_COMMERCE_REASON);
        }
    }

    private boolean isDuplicateStockAlias(String path) {
        return "/api/v1/inventory/{variantId}".equals(path)
                || "/api/v1/inventory/variants/{variantId}".equals(path);
    }

    private void markDeprecated(PathItem pathItem, String status, String reason) {
        for (Operation operation : pathItem.readOperations()) {
            if (operation == null) {
                continue;
            }
            operation.setDeprecated(true);
            operation.addExtension(EXTENSION_CONTRACT_STATUS, status);
            operation.addExtension(EXTENSION_DEPRECATION_REASON, reason);
        }
    }
}
