package com.company.platform.gateway.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Configures authentication and authorization behavior for gateway routes.
 */
@Configuration
@EnableWebFluxSecurity
@EnableConfigurationProperties(GatewayAuthProperties.class)
public class GatewaySecurityConfig {

    /**
     * Builds the reactive security filter chain based on gateway auth settings.
     *
     * @param http security DSL root
     * @param gatewayAuthProperties auth toggle/configuration properties
     * @return configured security filter chain
     */
    @Bean
    SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,
                                                  GatewayAuthProperties gatewayAuthProperties) {
        ServerHttpSecurity configuredHttp = http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable);

        if (gatewayAuthProperties.isEnabled()) {
            configuredHttp
                    .authorizeExchange(exchanges -> exchanges
                            .pathMatchers(
                                    "/actuator/health/**",
                                    "/actuator/info",
                                    "/internal/health",
                                    "/internal/app/**",
                                    "/internal/catalog-service/**",
                                    "/internal/search-service/**",
                                    "/internal/notification-service/**",
                                    "/internal/inventory-service/**",
                                    "/internal/pricing-service/**",
                                    "/internal/promotion-service/**",
                                    "/internal/review-service/**",
                                    "/internal/cart-service/**",
                                    "/internal/customer-service/**",
                                    "/internal/order-service/**",
                                    "/internal/checkout-service/**"
                            ).permitAll()
                            .anyExchange().authenticated())
                    .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        } else {
            configuredHttp.authorizeExchange(exchanges -> exchanges.anyExchange().permitAll());
        }

        return configuredHttp.build();
    }
}
