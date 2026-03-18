package com.noura.cart.controller;

import com.noura.cart.domain.enums.CartItemValidationStatus;
import com.noura.cart.dto.cart.CartItemResponse;
import com.noura.cart.dto.cart.CartResponse;
import com.noura.cart.dto.cart.CartTotalsResponse;
import com.noura.cart.exception.ApiExceptionHandler;
import com.noura.cart.service.CartService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web layer tests for {@link CartController}.
 */
@WebMvcTest(controllers = CartController.class)
@Import(ApiExceptionHandler.class)
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CartService cartService;

    /**
     * Verifies cart retrieval endpoint returns the standard API envelope.
     *
     * @throws Exception when MockMvc invocation fails
     */
    @Test
    void getCartReturnsApiEnvelope() throws Exception {
        UUID cartId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID productId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        CartResponse payload = new CartResponse(
                cartId,
                "GUEST",
                null,
                "guest-abc",
                "USD",
                null,
                null,
                List.of(new CartItemResponse(
                        UUID.fromString("33333333-3333-3333-3333-333333333333"),
                        productId,
                        null,
                        null,
                        null,
                        "Demo Product",
                        "SKU-1",
                        2,
                        new BigDecimal("4.5000"),
                        new BigDecimal("9.0000"),
                        new BigDecimal("30.0000"),
                        CartItemValidationStatus.VALID,
                        null,
                        Instant.parse("2026-03-16T10:15:30Z")
                )),
                new CartTotalsResponse(
                        new BigDecimal("9.0000"),
                        BigDecimal.ZERO.setScale(4),
                        BigDecimal.ZERO.setScale(4),
                        new BigDecimal("9.0000"),
                        null
                ),
                2,
                Instant.parse("2026-03-16T10:15:30Z")
        );
        when(cartService.getCart(any())).thenReturn(payload);

        mockMvc.perform(get("/api/v1/cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Cart"))
                .andExpect(jsonPath("$.data.cartId").value(cartId.toString()))
                .andExpect(jsonPath("$.data.items[0].productId").value(productId.toString()))
                .andExpect(header().string("X-Cart-Token", "guest-abc"));
    }

    /**
     * Verifies request validation errors return the standard error envelope.
     *
     * @throws Exception when MockMvc invocation fails
     */
    @Test
    void addItemRejectsInvalidQuantity() throws Exception {
        mockMvc.perform(post("/api/v1/cart/items")
                        .contentType("application/json")
                        .content("""
                                {
                                  "productId": "22222222-2222-2222-2222-222222222222",
                                  "quantity": 0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    /**
     * Verifies coupon remove endpoint returns a standard success envelope.
     *
     * @throws Exception when MockMvc invocation fails
     */
    @Test
    void removeCouponReturnsApiEnvelope() throws Exception {
        CartResponse payload = new CartResponse(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "GUEST",
                null,
                "guest-abc",
                "USD",
                null,
                null,
                List.of(),
                new CartTotalsResponse(
                        BigDecimal.ZERO.setScale(4),
                        BigDecimal.ZERO.setScale(4),
                        BigDecimal.ZERO.setScale(4),
                        BigDecimal.ZERO.setScale(4),
                        null
                ),
                0,
                Instant.parse("2026-03-16T10:15:30Z")
        );
        when(cartService.removeCoupon(any())).thenReturn(payload);

        mockMvc.perform(delete("/api/v1/cart/coupon"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Coupon removed"))
                .andExpect(jsonPath("$.data.totals.couponCode").value(nullValue()));
    }
}
