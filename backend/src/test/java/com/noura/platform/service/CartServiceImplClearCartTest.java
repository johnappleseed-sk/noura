package com.noura.platform.service;

import com.noura.platform.domain.entity.Cart;
import com.noura.platform.domain.entity.Store;
import com.noura.platform.domain.entity.UserAccount;
import com.noura.platform.dto.cart.CartDto;
import com.noura.platform.repository.CartItemRepository;
import com.noura.platform.repository.CartRepository;
import com.noura.platform.repository.ProductInventoryRepository;
import com.noura.platform.repository.ProductRepository;
import com.noura.platform.repository.StoreRepository;
import com.noura.platform.repository.UserAccountRepository;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceImplClearCartTest {

    @Mock
    private UserAccountRepository userAccountRepository;

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

    private CartServiceImpl cartService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "customer@noura.test",
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
                pricingService
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void clearCart_shouldRemoveItemsAndResetStoreContext() {
        UserAccount user = new UserAccount();
        user.setEmail("customer@noura.test");

        Store store = new Store();
        store.setId(UUID.randomUUID());
        store.setShippingFee(BigDecimal.valueOf(10));
        store.setFreeShippingThreshold(BigDecimal.valueOf(200));

        Cart cart = new Cart();
        UUID cartId = UUID.randomUUID();
        cart.setId(cartId);
        cart.setUser(user);
        cart.setStore(store);

        when(userAccountRepository.findByEmailIgnoreCase("customer@noura.test")).thenReturn(Optional.of(user));
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(cartId)).thenReturn(List.of());

        CartDto result = cartService.clearCart();

        assertEquals(cartId, result.cartId());
        assertNull(result.storeId());
        verify(cartItemRepository, times(1)).deleteByCartId(cartId);
        verify(cartRepository, times(1)).save(cart);
    }
}
