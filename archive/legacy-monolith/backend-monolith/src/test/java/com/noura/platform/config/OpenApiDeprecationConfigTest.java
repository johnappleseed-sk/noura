package com.noura.platform.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import org.junit.jupiter.api.Test;
import org.springdoc.core.customizers.OpenApiCustomizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenApiDeprecationConfigTest {

    @Test
    void marksTransitionalAndDeprecatedContractFamilies() {
        OpenApiDeprecationConfig config = new OpenApiDeprecationConfig();
        OpenApiCustomizer customizer = config.contractDeprecationCustomizer();

        OpenAPI openApi = new OpenAPI().paths(new Paths()
                .addPathItem("/api/v1/inventory/warehouses", new PathItem().get(new Operation()))
                .addPathItem("/api/v1/inventory/{variantId}", new PathItem().get(new Operation()))
                .addPathItem("/api/storefront/v1/orders", new PathItem().get(new Operation()))
                .addPathItem("/commerce/api/v1/reports", new PathItem().post(new Operation()))
                .addPathItem("/api/v1/products", new PathItem().get(new Operation())));

        customizer.customise(openApi);

        Operation inventoryAlias = openApi.getPaths().get("/api/v1/inventory/warehouses").getGet();
        assertTrue(Boolean.TRUE.equals(inventoryAlias.getDeprecated()));
        assertEquals("transitional", inventoryAlias.getExtensions().get("x-contract-status"));

        Operation duplicateAlias = openApi.getPaths().get("/api/v1/inventory/{variantId}").getGet();
        assertTrue(Boolean.TRUE.equals(duplicateAlias.getDeprecated()));
        assertEquals("transitional", duplicateAlias.getExtensions().get("x-contract-status"));
        assertEquals(
                "Duplicate stock aliases. Keep one canonical inventory path under /api/inventory/v1/*.",
                duplicateAlias.getExtensions().get("x-deprecation-reason")
        );

        Operation legacyStorefront = openApi.getPaths().get("/api/storefront/v1/orders").getGet();
        assertTrue(Boolean.TRUE.equals(legacyStorefront.getDeprecated()));
        assertEquals("deprecated", legacyStorefront.getExtensions().get("x-contract-status"));

        Operation legacyCommerce = openApi.getPaths().get("/commerce/api/v1/reports").getPost();
        assertTrue(Boolean.TRUE.equals(legacyCommerce.getDeprecated()));
        assertEquals("deprecated", legacyCommerce.getExtensions().get("x-contract-status"));

        Operation canonicalOperation = openApi.getPaths().get("/api/v1/products").getGet();
        assertFalse(Boolean.TRUE.equals(canonicalOperation.getDeprecated()));
    }
}
