package com.noura.platform.security;

import com.noura.platform.config.AppProperties;
import com.noura.platform.config.RequestCorrelationFilter;
import com.noura.platform.config.RateLimitFilter;
import com.noura.platform.config.SecurityConfig;
import com.noura.platform.controller.OrderController;
import com.noura.platform.domain.enums.RoleType;
import com.noura.platform.dto.cart.AddCartItemRequest;
import com.noura.platform.dto.cart.ApplyCouponRequest;
import com.noura.platform.dto.cart.CartDto;
import com.noura.platform.dto.cart.UpdateCartItemRequest;
import com.noura.platform.dto.order.CheckoutConfirmRequest;
import com.noura.platform.dto.order.CheckoutPaymentRequest;
import com.noura.platform.dto.order.CheckoutRequest;
import com.noura.platform.dto.order.CheckoutShippingRequest;
import com.noura.platform.dto.order.CheckoutStepPreviewDto;
import com.noura.platform.dto.order.OrderDto;
import com.noura.platform.dto.order.OrderTimelineEventDto;
import com.noura.platform.dto.order.UpdateOrderStatusRequest;
import com.noura.platform.dto.storefront.StorefrontAddCartItemRequest;
import com.noura.platform.dto.storefront.StorefrontCartDto;
import com.noura.platform.dto.storefront.StorefrontCheckoutRequest;
import com.noura.platform.dto.storefront.StorefrontOrderResult;
import com.noura.platform.dto.storefront.StorefrontOrderSummaryDto;
import com.noura.platform.dto.storefront.StorefrontUpdateCartItemRequest;
import com.noura.platform.service.UnifiedOrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
        "app.jwt.issuer=noura-test"
})
class OrderControllerSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(OrderController.class)
    static class TestApplication {
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        UnifiedOrderService unifiedOrderService() {
            return new UnifiedOrderService() {
                @Override
                @PreAuthorize("hasRole('ADMIN')")
                public Page<OrderDto> adminOrders(Pageable pageable) {
                    return Page.empty(pageable);
                }

                @Override
                public OrderDto getPlatformOrderById(UUID orderId) {
                    return null;
                }

                @Override
                public List<OrderTimelineEventDto> orderTimeline(UUID orderId) {
                    return List.of();
                }

                @Override
                public OrderDto updateOrderStatus(UUID orderId, UpdateOrderStatusRequest request) {
                    return null;
                }

                @Override
                public CheckoutStepPreviewDto reviewCheckoutStep() {
                    return null;
                }

                @Override
                public CheckoutStepPreviewDto shippingCheckoutStep(CheckoutShippingRequest request) {
                    return null;
                }

                @Override
                public CheckoutStepPreviewDto paymentCheckoutStep(CheckoutPaymentRequest request) {
                    return null;
                }

                @Override
                public OrderDto confirmCheckout(CheckoutConfirmRequest request) {
                    return null;
                }

                @Override
                public OrderDto checkout(CheckoutRequest request) {
                    return null;
                }

                @Override
                public CartDto getMyCart() {
                    return null;
                }

                @Override
                public CartDto addCartItem(AddCartItemRequest request) {
                    return null;
                }

                @Override
                public CartDto updateCartItem(UUID cartItemId, UpdateCartItemRequest request) {
                    return null;
                }

                @Override
                public CartDto removeCartItem(UUID cartItemId) {
                    return null;
                }

                @Override
                public CartDto clearCart() {
                    return null;
                }

                @Override
                public CartDto applyCoupon(ApplyCouponRequest request) {
                    return null;
                }

                @Override
                public List<OrderDto> myOrderHistory() {
                    return List.of();
                }

                @Override
                public List<OrderDto> quickReorder(UUID orderId) {
                    return List.of();
                }

                @Override
                public StorefrontOrderResult checkoutStorefront(Long customerId, StorefrontCheckoutRequest request) {
                    return null;
                }

                @Override
                public List<StorefrontOrderSummaryDto> listStorefrontOrders(Long customerId) {
                    return List.of();
                }

                @Override
                public StorefrontOrderResult getStorefrontOrder(Long customerId, Long orderId) {
                    return null;
                }

                @Override
                public StorefrontOrderResult cancelStorefrontOrder(Long customerId, Long orderId) {
                    return null;
                }

                @Override
                public StorefrontCartDto getOrCreateStorefrontCart(Long customerId) {
                    return null;
                }

                @Override
                public StorefrontCartDto addStorefrontCartItem(Long customerId, StorefrontAddCartItemRequest request) {
                    return null;
                }

                @Override
                public StorefrontCartDto updateStorefrontCartItem(Long customerId, Long itemId, StorefrontUpdateCartItemRequest request) {
                    return null;
                }

                @Override
                public void removeStorefrontCartItem(Long customerId, Long itemId) {
                }

                @Override
                public StorefrontCartDto clearStorefrontCart(Long customerId) {
                    return null;
                }
            };
        }
    }

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

}
