package com.company.platform.gateway.health;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
public class GatewayHealthController {

    @GetMapping("/internal/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "service", "edge-gateway",
                "timestamp", Instant.now().toString()
        );
    }
}
