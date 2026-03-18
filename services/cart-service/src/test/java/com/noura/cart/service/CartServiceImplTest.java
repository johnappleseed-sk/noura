package com.noura.cart.service;

import com.noura.cart.domain.entity.Cart;
import com.noura.cart.domain.entity.CartItem;
import com.noura.cart.domain.enums.CartItemValidationStatus;
import com.noura.cart.domain.enums.CartOwnerType;
import com.noura.cart.domain.enums.CartStatus;
import com.noura.cart.dto.cart.AddCartItemRequest;
import com.noura.cart.dto.cart.CartResponse;
import com.noura.cart.exception.CartOperationException;
import com.noura.cart.integration.CatalogGateway;
import com.noura.cart.integration.InventoryGateway;
import com.noura.cart.integration.PromotionGateway;
import com.noura.cart.integration.PricingGateway;
import com.noura.cart.integration.model.InventorySnapshot;
import com.noura.cart.integration.model.PricingSnapshot;
import com.noura.cart.integration.model.ProductSnapshot;
import com.noura.cart.repository.CartItemRepository;
import com.noura.cart.repository.CartRepository;
import com.noura.cart.service.impl.CartServiceImpl;
import com.noura.cart.service.model.CartContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CartServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private CatalogGateway catalogGateway;

    @Mock
    private PricingGateway pricingGateway;

    @Mock
    private InventoryGateway inventoryGateway;

    @Mock
    private PromotionGateway promotionGateway;

    private CartServiceImpl cartService;

    /**
     * Initializes service under test with mocked dependencies.
     */
    @BeforeEach
    void setUp() {
        cartService = new CartServiceImpl(
                cartRepository,
                cartItemRepository,
                catalogGateway,
                pricingGateway,
                inventoryGateway,
                promotionGateway
        );
    }

    /**
     * Verifies add-item command merges with existing line and recomputes subtotal.
     */
    @Test
    void addItemMergesExistingLineAndRecomputesTotals() {
        UUID cartId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        CartContext context = CartContext.customer("customer-1");
        Cart cart = customerCart(cartId, context.customerId());

        CartItem existing = new CartItem();
        existing.setId(UUID.randomUUID());
        existing.setCart(cart);
        existing.setCartId(cartId);
        existing.setProductId(productId);
        existing.setStoreId(storeId);
        existing.setQuantity(1);
        existing.setUnitPrice(new BigDecimal("5.0000"));
        existing.setLineTotal(new BigDecimal("5.0000"));
        existing.setProductNameSnapshot("Demo Product");
        existing.setCurrencyCode("USD");
        existing.setValidationStatus(CartItemValidationStatus.VALID);
        existing.setUpdatedAt(Instant.now());

        when(cartRepository.findActiveCustomerCartForUpdate(context.customerId())).thenReturn(Optional.of(cart));
        when(cartItemRepository.findDeduplicatedItemForUpdate(cartId, productId, null, storeId)).thenReturn(Optional.of(existing));
        when(catalogGateway.findProduct(productId)).thenReturn(Optional.of(new ProductSnapshot(
                productId,
                "Demo Product",
                null,
                "SKU-001",
                false
        )));
        when(pricingGateway.resolvePrice(productId, storeId)).thenReturn(Optional.of(new PricingSnapshot(
                productId,
                new BigDecimal("5.0000"),
                "USD"
        )));
        when(inventoryGateway.resolveAvailability(productId, storeId)).thenReturn(new InventorySnapshot(
                productId,
                storeId,
                new BigDecimal("25.0000"),
                true
        ));
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(cartItemRepository.findByCartIdOrderByCreatedAtAsc(cartId)).thenReturn(List.of(existing));

        CartResponse response = cartService.addItem(context, new AddCartItemRequest(
                productId,
                null,
                storeId,
                2,
                null,
                null,
                null
        ));

        assertThat(response.itemCount()).isEqualTo(3);
        assertThat(response.totals().subtotal()).isEqualByComparingTo("15.0000");
        assertThat(response.items().getFirst().quantity()).isEqualTo(3);
        assertThat(response.items().getFirst().validationStatus()).isEqualTo(CartItemValidationStatus.VALID);
    }

    /**
     * Verifies refresh marks line as UNKNOWN when strict mode is disabled and inventory dependency fails.
     */
    @Test
    void refreshMarksLineUnknownWhenDependencyUnavailable() {
        UUID cartId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        CartContext context = CartContext.customer("customer-2");
        Cart cart = customerCart(cartId, context.customerId());

        CartItem line = new CartItem();
        line.setId(UUID.randomUUID());
        line.setCart(cart);
        line.setCartId(cartId);
        line.setProductId(productId);
        line.setQuantity(2);
        line.setUnitPrice(new BigDecimal("9.9900"));
        line.setLineTotal(new BigDecimal("19.9800"));
        line.setCurrencyCode("USD");
        line.setProductNameSnapshot("Dependency Product");
        line.setValidationStatus(CartItemValidationStatus.VALID);
        line.setUpdatedAt(Instant.now());

        when(cartRepository.findActiveCustomerCartForUpdate(context.customerId())).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdOrderByCreatedAtAsc(cartId)).thenReturn(List.of(line));
        when(catalogGateway.findProduct(productId)).thenReturn(Optional.of(new ProductSnapshot(
                productId,
                "Dependency Product",
                null,
                "SKU-002",
                false
        )));
        when(pricingGateway.resolvePrice(productId, null)).thenReturn(Optional.of(new PricingSnapshot(
                productId,
                new BigDecimal("9.9900"),
                "USD"
        )));
        when(inventoryGateway.resolveAvailability(productId, null)).thenThrow(new CartOperationException(
                HttpStatus.BAD_GATEWAY,
                "INVENTORY_SERVICE_UNREACHABLE",
                "inventory down"
        ));
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartResponse response = cartService.refresh(context, false);

        assertThat(response.items().getFirst().validationStatus()).isEqualTo(CartItemValidationStatus.UNKNOWN);
        assertThat(response.totals().subtotal()).isEqualByComparingTo("0.0000");
    }

    /**
     * Creates customer cart fixture.
     *
     * @param cartId cart identifier
     * @param customerId customer identifier
     * @return prepared cart fixture
     */
    private Cart customerCart(UUID cartId, String customerId) {
        Cart cart = new Cart();
        cart.setId(cartId);
        cart.setOwnerType(CartOwnerType.CUSTOMER);
        cart.setCustomerId(customerId);
        cart.setStatus(CartStatus.ACTIVE);
        cart.setCurrencyCode("USD");
        cart.setSubtotal(BigDecimal.ZERO.setScale(4));
        cart.setDiscountAmount(BigDecimal.ZERO.setScale(4));
        cart.setShippingAmount(BigDecimal.ZERO.setScale(4));
        cart.setTotalAmount(BigDecimal.ZERO.setScale(4));
        cart.setUpdatedAt(Instant.now());
        return cart;
    }
}
