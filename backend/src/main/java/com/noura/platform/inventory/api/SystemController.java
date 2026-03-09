package com.noura.platform.inventory.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("${inventory.api.base-path:/api/inventory/v1}/system")
public class SystemController {

    @Value("${spring.application.name:noura-inventory-service}")
    private String applicationName;

    @Value("${inventory.foundation.version:phase-1}")
    private String foundationVersion;

    @Value("${spring.profiles.active:inventory-local}")
    private String activeProfiles;

    @Value("${spring.datasource.url:jdbc:mysql://localhost:3306/noura_inventory}")
    private String datasourceUrl;

    @GetMapping("/status")
    public InventorySystemStatusResponse status() {
        return new InventorySystemStatusResponse(
                applicationName,
                foundationVersion,
                "UP",
                activeProfiles,
                datasourceUrl,
                OffsetDateTime.now()
        );
    }

    public record InventorySystemStatusResponse(
            String service,
            String foundationVersion,
            String status,
            String activeProfiles,
            String datasourceUrl,
            OffsetDateTime timestamp
    ) {
    }
}
