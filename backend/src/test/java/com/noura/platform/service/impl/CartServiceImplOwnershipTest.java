package com.noura.platform.service.impl;

import com.noura.platform.common.exception.UnauthorizedException;
import com.noura.platform.domain.entity.Cart;
import com.noura.platform.domain.entity.CartItem;
import com.noura.platform.domain.entity.UserAccount;
import com.noura.platform.dto.cart.UpdateCartItemRequest;
import com.noura.platform.repository.CartItemRepository;
import com.noura.platform.repository.CartRepository;
import com.noura.platform.repository.ProductInventoryRepository;
import com.noura.platform.repository.ProductRepository;
import com.noura.platform.repository.StoreRepository;
import com.noura.platform.repository.UserAccountRepository;
import com.noura.platform.repository.AddressRepository;
import com.noura.platform.service.AnalyticsEventService;
import com.noura.platform.service.PricingService;
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

    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private AddressRepository addressRepository;
    @Mock
    private CartRepository cartRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private ProductInventoryRepository inventoryRepository;
    @Mock
    private StoreRepository storeRepository;
    @Mock
    private PricingService pricingService;
    @Mock
    private AnalyticsEventService analyticsEventService;

    private CartServiceImpl cartService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "other-user@noura.test",
                        "n/a",
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                )
        );
        cartService = new CartServiceImpl(
                userAccountRepository,
                addressRepository,
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
    void updateItem_shouldRejectForeignCartItemOwner() {
        UUID cartItemId = UUID.randomUUID();
        CartItem foreignItem = foreignOwnedCartItem();
        when(cartItemRepository.findById(cartItemId)).thenReturn(Optional.of(foreignItem));

        assertThrows(
                UnauthorizedException.class,
                () -> cartService.updateItem(cartItemId, new UpdateCartItemRequest(2))
        );

        verify(cartItemRepository, never()).save(foreignItem);
    }

    @Test
    void removeItem_shouldRejectForeignCartItemOwner() {
        UUID cartItemId = UUID.randomUUID();
        CartItem foreignItem = foreignOwnedCartItem();
        when(cartItemRepository.findById(cartItemId)).thenReturn(Optional.of(foreignItem));

        assertThrows(UnauthorizedException.class, () -> cartService.removeItem(cartItemId));

        verify(cartItemRepository, never()).delete(foreignItem);
    }

    private CartItem foreignOwnedCartItem() {
        UserAccount owner = new UserAccount();
        owner.setEmail("owner@noura.test");
        Cart cart = new Cart();
        cart.setUser(owner);
        CartItem item = new CartItem();
        item.setCart(cart);
        item.setQuantity(1);
        return item;
    }
}
