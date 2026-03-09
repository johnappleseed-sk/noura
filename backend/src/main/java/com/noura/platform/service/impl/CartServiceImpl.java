package com.noura.platform.service.impl;

import com.noura.platform.common.exception.NotFoundException;
import com.noura.platform.common.exception.UnauthorizedException;
import com.noura.platform.domain.entity.Cart;
import com.noura.platform.domain.entity.CartItem;
import com.noura.platform.domain.entity.Product;
import com.noura.platform.domain.entity.ProductInventory;
import com.noura.platform.domain.entity.Store;
import com.noura.platform.domain.entity.UserAccount;
import com.noura.platform.dto.cart.AddCartItemRequest;
import com.noura.platform.dto.cart.ApplyCouponRequest;
import com.noura.platform.dto.cart.CartDto;
import com.noura.platform.dto.cart.CartItemDto;
import com.noura.platform.dto.cart.CartTotalsDto;
import com.noura.platform.dto.cart.UpdateCartItemRequest;
import com.noura.platform.dto.order.CheckoutPaymentRequest;
import com.noura.platform.dto.order.CheckoutShippingRequest;
import com.noura.platform.repository.CartItemRepository;
import com.noura.platform.repository.CartRepository;
import com.noura.platform.repository.ProductInventoryRepository;
import com.noura.platform.repository.ProductRepository;
import com.noura.platform.repository.StoreRepository;
import com.noura.platform.repository.UserAccountRepository;
import com.noura.platform.service.PricingService;
import com.noura.platform.security.SecurityUtils;
import com.noura.platform.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final UserAccountRepository userAccountRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final ProductInventoryRepository inventoryRepository;
    private final StoreRepository storeRepository;
    private final PricingService pricingService;

    /**
     * Retrieves my cart.
     *
     * @return The mapped DTO representation.
     */
    @Override
    public CartDto getMyCart() {
        Cart cart = currentCart();
        return toCartDto(cart);
    }

    /**
     * Adds item.
     *
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    @Override
    @Transactional
    public CartDto addItem(AddCartItemRequest request) {
        Cart cart = currentCart();
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new NotFoundException("PRODUCT_NOT_FOUND", "Product not found"));

        Store effectiveStore = cart.getStore();
        if (request.storeId() != null) {
            if (cart.getStore() != null && !cart.getStore().getId().equals(request.storeId())) {
                throw new UnauthorizedException("STORE_CONFLICT", "Cart already belongs to another store");
            }
            Store store = storeRepository.findById(request.storeId())
                    .orElseThrow(() -> new NotFoundException("STORE_NOT_FOUND", "Store not found"));
            cart.setStore(store);
            cartRepository.save(cart);
            effectiveStore = store;
        }
        CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId())
                .orElseGet(() -> {
                    CartItem created = new CartItem();
                    created.setCart(cart);
                    created.setProduct(product);
                    created.setQuantity(0);
                    created.setUnitPrice(resolvePrice(product, cart.getStore()));
                    return created;
                });
        int nextQuantity = item.getQuantity() + request.quantity();
        if (effectiveStore != null) {
            validateStock(product.getId(), effectiveStore.getId(), nextQuantity);
        }
        item.setQuantity(nextQuantity);
        cartItemRepository.save(item);
        return toCartDto(cart);
    }

    /**
     * Updates item.
     *
     * @param cartItemId The cart item id used to locate the target record.
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    @Override
    @Transactional
    public CartDto updateItem(UUID cartItemId, UpdateCartItemRequest request) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new NotFoundException("CART_ITEM_NOT_FOUND", "Cart item not found"));
        if (!item.getCart().getUser().getEmail().equalsIgnoreCase(SecurityUtils.currentEmail())) {
            throw new UnauthorizedException("CART_FORBIDDEN", "Cart item ownership mismatch");
        }
        if (item.getCart().getStore() != null) {
            validateStock(item.getProduct().getId(), item.getCart().getStore().getId(), request.quantity());
        }
        item.setQuantity(request.quantity());
        cartItemRepository.save(item);
        return toCartDto(item.getCart());
    }

    /**
     * Removes item.
     *
     * @param cartItemId The cart item id used to locate the target record.
     * @return The mapped DTO representation.
     */
    @Override
    @Transactional
    public CartDto removeItem(UUID cartItemId) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new NotFoundException("CART_ITEM_NOT_FOUND", "Cart item not found"));
        if (!item.getCart().getUser().getEmail().equalsIgnoreCase(SecurityUtils.currentEmail())) {
            throw new UnauthorizedException("CART_FORBIDDEN", "Cart item ownership mismatch");
        }
        Cart cart = item.getCart();
        cartItemRepository.delete(item);
        return toCartDto(cart);
    }

    /**
     * Clears cart.
     *
     * @return The mapped DTO representation.
     */
    @Override
    @Transactional
    public CartDto clearCart() {
        Cart cart = currentCart();
        cartItemRepository.deleteByCartId(cart.getId());
        cart.setStore(null);
        cart.setFulfillmentMethod(null);
        cart.setShippingAddressSnapshot(null);
        cart.setPaymentReference(null);
        cart.setCouponCode(null);
        cart.setB2bInvoice(false);
        cart.setIdempotencyKey(null);
        cartRepository.save(cart);
        return toCartDto(cart);
    }

    /**
     * Applies coupon.
     *
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    @Override
    public CartDto applyCoupon(ApplyCouponRequest request) {
        String normalized = normalizeCouponCode(request.couponCode());
        Cart cart = currentCart();
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
        pricingService.calculateTotals(items, cart.getStore(), normalized);
        cart.setCouponCode(normalized);
        cartRepository.save(cart);
        return toCartDto(cart);
    }

    /**
     * Applies shipping draft fields.
     */
    @Override
    @Transactional
    public CartDto updateShippingDraft(CheckoutShippingRequest request) {
        Cart cart = currentCart();
        if (request.storeId() != null) {
            if (cart.getStore() != null && !cart.getStore().getId().equals(request.storeId())) {
                throw new UnauthorizedException("STORE_CONFLICT", "Cart already belongs to another store");
            }
            Store store = storeRepository.findById(request.storeId())
                    .orElseThrow(() -> new NotFoundException("STORE_NOT_FOUND", "Store not found"));
            cart.setStore(store);
        }
        cart.setFulfillmentMethod(request.fulfillmentMethod());
        cart.setShippingAddressSnapshot(request.shippingAddressSnapshot().trim());
        cartRepository.save(cart);
        return toCartDto(cart);
    }

    /**
     * Applies payment draft fields.
     */
    @Override
    @Transactional
    public CartDto updatePaymentDraft(CheckoutPaymentRequest request) {
        Cart cart = currentCart();
        String normalizedCoupon = normalizeCouponCode(request.couponCode());
        if (normalizedCoupon != null) {
            List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
            pricingService.calculateTotals(items, cart.getStore(), normalizedCoupon);
            cart.setCouponCode(normalizedCoupon);
        }
        cart.setPaymentReference(normalizeNullable(request.paymentReference()));
        cart.setB2bInvoice(request.b2bInvoice());
        cart.setIdempotencyKey(normalizeNullable(request.idempotencyKey()));
        cartRepository.save(cart);
        return toCartDto(cart);
    }

    /**
     * Executes current cart.
     *
     * @return The result of current cart.
     */
    private Cart currentCart() {
        UserAccount user = userAccountRepository.findByEmailIgnoreCase(SecurityUtils.currentEmail())
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
        return cartRepository.findByUser(user).orElseGet(() -> {
            Cart created = new Cart();
            created.setUser(user);
            return cartRepository.save(created);
        });
    }

    /**
     * Maps source data to CartDto.
     *
     * @param cart The cart value.
     * @param couponCode The coupon code value.
     * @return The mapped DTO representation.
     */
    private CartDto toCartDto(Cart cart) {
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
        List<CartItemDto> lines = items.stream()
                .map(item -> new CartItemDto(
                        item.getId(),
                        item.getProduct().getId(),
                        item.getProduct().getName(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
                ))
                .toList();
        CartTotalsDto totals = pricingService.calculateTotals(items, cart.getStore(), cart.getCouponCode());
        return new CartDto(cart.getId(), cart.getStore() == null ? null : cart.getStore().getId(), lines, totals);
    }

    private String normalizeCouponCode(String couponCode) {
        if (couponCode == null || couponCode.isBlank()) {
            return null;
        }
        return couponCode.trim().toUpperCase();
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * Executes resolve price.
     *
     * @param product The product value.
     * @param store The store value.
     * @return The result of resolve price.
     */
    private BigDecimal resolvePrice(Product product, Store store) {
        if (store == null) {
            return product.getBasePrice();
        }
        return inventoryRepository.findByProductIdAndStoreId(product.getId(), store.getId())
                .map(ProductInventory::getStorePrice)
                .orElse(product.getBasePrice());
    }

    /**
     * Validates validate stock.
     *
     * @param productId The product id used to locate the target record.
     * @param storeId The store id used to locate the target record.
     * @param quantity The quantity value.
     */
    private void validateStock(UUID productId, UUID storeId, int quantity) {
        ProductInventory inventory = inventoryRepository.findByProductIdAndStoreId(productId, storeId)
                .orElseThrow(() -> new NotFoundException("INVENTORY_NOT_FOUND", "No inventory for selected store"));
        if (inventory.getStock() < quantity) {
            throw new UnauthorizedException("STOCK_INSUFFICIENT", "Not enough stock at selected store");
        }
    }
}
