package com.noura.platform.security;

import com.noura.platform.config.AppProperties;
import com.noura.platform.config.RequestCorrelationFilter;
import com.noura.platform.config.RateLimitFilter;
import com.noura.platform.config.SecurityConfig;
import com.noura.platform.domain.enums.RoleType;
import com.noura.platform.dto.order.OrderDto;
import com.noura.platform.dto.order.OrderTimelineEventDto;
import com.noura.platform.dto.order.UpdateOrderStatusRequest;
import com.noura.platform.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = com.noura.platform.controller.OrderController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtTokenProvider.class,
        RequestCorrelationFilter.class,
        RateLimitFilter.class,
        OrderControllerSecurityIntegrationTest.TestConfig.class
})
@TestPropertySource(properties = {
        "app.api.version-prefix=/api/v1",
        "app.jwt.secret=0123456789abcdef0123456789abcdef",
        "app.jwt.issuer=noura-test",
        "spring.main.allow-bean-definition-overriding=true"
})
class OrderControllerSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void adminOrders_shouldRejectAnonymousRequest() throws Exception {
        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH_REQUIRED"))
                .andExpect(header().exists("X-Correlation-ID"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Content-Security-Policy", containsString("default-src 'self'")))
                .andExpect(header().string("Strict-Transport-Security", containsString("max-age=31536000")));
    }

    @Test
    void adminOrders_shouldRejectNonAdminUser() throws Exception {
        mockMvc.perform(get("/api/v1/orders")
                        .with(user("customer@noura.test").roles("CUSTOMER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }

    @Test
    void adminOrders_shouldRejectJwtWithInvalidSignature() throws Exception {
        String invalidSignatureToken = tokenSignedWithDifferentSecret();

        mockMvc.perform(get("/api/v1/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + invalidSignatureToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH_REQUIRED"));
    }

    private String tokenSignedWithDifferentSecret() {
        AppProperties properties = new AppProperties();
        properties.getJwt().setSecret("abcdef0123456789abcdef0123456789");
        properties.getJwt().setIssuer("noura-test");
        properties.getJwt().setAccessTokenValidityMinutes(30);
        JwtTokenProvider provider = new JwtTokenProvider(properties);
        return provider.generateAccessToken(
                UUID.randomUUID(),
                "customer@noura.test",
                Set.of(RoleType.CUSTOMER)
        );
    }

    @org.springframework.boot.test.context.TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        CustomUserDetailsService customUserDetailsService() {
            return new CustomUserDetailsService(null) {
                @Override
                public UserDetails loadUserByUsername(String email) {
                    return User.withUsername(email)
                            .password("n/a")
                            .authorities("ROLE_CUSTOMER")
                            .build();
                }
            };
        }

        @Bean
        @Primary
        OrderService orderService() {
            return new OrderService() {
                @Override
                @PreAuthorize("hasRole('ADMIN')")
                public Page<OrderDto> adminOrders(Pageable pageable) {
                    return Page.empty(pageable);
                }

                @Override
                @PreAuthorize("hasRole('ADMIN')")
                public OrderDto getById(UUID orderId) {
                    return null;
                }

                @Override
                @PreAuthorize("hasRole('ADMIN')")
                public List<OrderTimelineEventDto> orderTimeline(UUID orderId) {
                    return List.of();
                }

                @Override
                @PreAuthorize("hasRole('ADMIN')")
                public OrderDto updateStatus(UUID orderId, UpdateOrderStatusRequest request) {
                    return null;
                }
            };
        }
    }
}
