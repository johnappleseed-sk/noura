package com.noura.platform.inventory.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.noura.platform.common.api.ApiResponse;
import com.noura.platform.inventory.security.InventoryJwtAuthenticationFilter;
import com.noura.platform.inventory.security.InventorySecurityProperties;
import com.noura.platform.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.core.annotation.Order;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableConfigurationProperties(InventorySecurityProperties.class)
public class InventorySecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain inventorySecurityFilterChain(HttpSecurity http,
                                                            InventoryJwtAuthenticationFilter jwtAuthenticationFilter,
                                                            InventoryHeaderAuthenticationFilter headerAuthenticationFilter,
                                                            JwtAuthenticationFilter platformJwtAuthenticationFilter,
                                                            InventorySecurityProperties securityProperties,
                                                            ObjectMapper objectMapper) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, ex) -> writeError(
                                objectMapper,
                                response,
                                HttpStatus.UNAUTHORIZED,
                                ApiResponse.fail(
                                        "Authentication required",
                                        "AUTH_REQUIRED",
                                        securityProperties.isDevHeaderAuthEnabled()
                                                ? "Supply a Bearer token or local dev inventory headers"
                                                : "Supply a valid Bearer token",
                                        request.getRequestURI()
                                )
                        ))
                        .accessDeniedHandler((request, response, ex) -> writeError(
                                objectMapper,
                                response,
                                HttpStatus.FORBIDDEN,
                                ApiResponse.fail(
                                        "Access denied",
                                        "ACCESS_DENIED",
                                        "You do not have permission to access this inventory resource",
                                        request.getRequestURI()
                                )
                        ))
                )
                .securityMatcher("/api/inventory/v1/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/inventory/v1/system/**").permitAll()
                        .requestMatchers("/api/inventory/v1/auth/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(headerAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        // Also accept the platform JWT on inventory routes so the monolith can use a single login.
        http.addFilterBefore(platformJwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private void writeError(ObjectMapper objectMapper,
                            HttpServletResponse response,
                            HttpStatus status,
                            ApiResponse<Void> body) throws java.io.IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        objectMapper.writeValue(response.getWriter(), body);
    }
}
