package com.noura.platform.service;

import com.noura.platform.common.exception.UnauthorizedException;
import com.noura.platform.domain.entity.Cart;
import com.noura.platform.domain.entity.CartItem;
import com.noura.platform.domain.entity.UserAccount;
import com.noura.platform.dto.cart.UpdateCartItemRequest;
import com.noura.platform.repository.*;
import com.noura.platform.service.AnalyticsEventService;
import com.noura.platform.service.PricingService;
import com.noura.platform.service.impl.CartServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceImplOwnershipTest {

    @Mock private UserAccountRepository userAccountRepository;
    @Mock private CartRepository cartRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private ProductRepository productRepository;
    @Mock private ProductInventoryRepository inventoryRepository;
    @Mock private StoreRepository storeRepository;
    @Mock private PricingService pricingService;
    @Mock private AnalyticsEventService analyticsEventService;

    private CartServiceImpl cartService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "owner-a@noura.test",
                        "n/a",
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                )
        );
        cartService = new CartServiceImpl(
                userAccountRepository,
                cartRepository,
                cartItemRepository,
                productRepository,
                inventoryRepository,
                storeRepository,
                pricingService,
                analyticsEventService
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void updateItem_shouldRejectWhenCartBelongsToAnotherUser() {
        UserAccount ownerB = new UserAccount();
        ownerB.setEmail("owner-b@noura.test");
        Cart cart = new Cart();
        cart.setUser(ownerB);

        CartItem item = new CartItem();
        item.setId(UUID.randomUUID());
        item.setCart(cart);

        when(cartItemRepository.findById(item.getId())).thenReturn(Optional.of(item));

        assertThrows(
                UnauthorizedException.class,
                () -> cartService.updateItem(item.getId(), new UpdateCartItemRequest(2))
        );
        verify(cartItemRepository, never()).save(item);
    }
}
