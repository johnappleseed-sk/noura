package com.noura.platform.controller;

import com.noura.platform.common.api.ApiResponse;
import com.noura.platform.dto.cart.CartDto;
import com.noura.platform.dto.cart.CartTotalsDto;
import com.noura.platform.service.CartService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartControllerTest {

    @Mock
    private CartService cartService;

    @Test
    void clearItems_shouldReturnUpdatedCart() {
        CartController controller = new CartController(cartService);
        CartDto cart = new CartDto(
                UUID.randomUUID(),
                null,
                List.of(),
                new CartTotalsDto(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null)
        );
        when(cartService.clearCart()).thenReturn(cart);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/cart/items");

        ApiResponse<CartDto> response = controller.clearItems(request);

        assertEquals("Cart cleared", response.getMessage());
        assertSame(cart, response.getData());
        verify(cartService, times(1)).clearCart();
    }
}

