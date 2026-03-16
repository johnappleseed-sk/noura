package com.noura.platform.service.impl;

import com.noura.platform.common.exception.BadRequestException;
import com.noura.platform.common.exception.ForbiddenException;
import com.noura.platform.common.exception.NotFoundException;
import com.noura.platform.domain.entity.Address;
import com.noura.platform.domain.entity.Cart;
import com.noura.platform.domain.entity.CartItem;
import com.noura.platform.domain.entity.Product;
import com.noura.platform.domain.entity.ProductInventory;
import com.noura.platform.domain.entity.Store;
import com.noura.platform.domain.entity.StoreProductReference;
import com.noura.platform.domain.entity.UserAccount;
import com.noura.platform.dto.cart.AddCartItemRequest;
import com.noura.platform.dto.cart.ApplyCouponRequest;
import com.noura.platform.dto.cart.CartDto;
import com.noura.platform.dto.cart.CartItemDto;
import com.noura.platform.dto.cart.CartItemResponse;
import com.noura.platform.dto.cart.CartResponse;
import com.noura.platform.dto.cart.CartTotalsDto;
import com.noura.platform.dto.cart.UpdateCartItemRequest;
import com.noura.platform.dto.order.CheckoutPaymentRequest;
import com.noura.platform.dto.order.CheckoutShippingRequest;
import com.noura.platform.repository.AddressRepository;
import com.noura.platform.repository.CartItemRepository;
import com.noura.platform.repository.CartRepository;
import com.noura.platform.repository.ProductInventoryRepository;
import com.noura.platform.repository.StoreProductReferenceRepository;
import com.noura.platform.repository.StoreRepository;
import com.noura.platform.repository.UserAccountRepository;
import com.noura.platform.security.SecurityUtils;
import com.noura.platform.service.CartService;
import com.noura.platform.service.PricingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final UserAccountRepository userAccountRepository;
    private final AddressRepository addressRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final StoreProductReferenceRepository storeProductReferenceRepository;
    private final ProductInventoryRepository inventoryRepository;
    private final StoreRepository storeRepository;
    private final PricingService pricingService;

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public CartResponse getCart() {
        return toCartResponse(currentCart());
    }

    @Override
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public CartResponse addCartItem(AddCartItemRequest request) {
        Cart cart = currentCart();
        StoreProductReference reference = requireActiveStoreProductReference(request.storeProductReferenceId());
        ensureCartStoreCompatible(cart, reference.getStore());

        CartItem item = cartItemRepository.findByCartIdAndStoreProductReferenceId(cart.getId(), reference.getId())
                .orElseGet(() -> createCartItem(cart, reference));

        int nextQuantity = item.getQuantity() + request.quantity();
        validatePositiveQuantity(nextQuantity);
        validateAvailability(reference, nextQuantity);

        item.setQuantity(nextQuantity);
        item.setUnitPrice(resolveUnitPrice(reference));
        item.setProduct(reference.getProduct());
        item.setStoreProductReference(reference);
        cartItemRepository.save(item);
        return toCartResponse(cart);
    }

    @Override
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public CartResponse updateCartItem(UUID cartItemId, UpdateCartItemRequest request) {
        CartItem item = requireOwnedItem(cartItemId);
        validatePositiveQuantity(request.quantity());

        StoreProductReference reference = resolveStoreProductReference(item);
        if (reference != null) {
            validateAvailability(reference, request.quantity());
            item.setStoreProductReference(reference);
            item.setProduct(reference.getProduct());
            item.setUnitPrice(resolveUnitPrice(reference));
        }

        item.setQuantity(request.quantity());
        cartItemRepository.save(item);
        return toCartResponse(item.getCart());
    }

    @Override
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public CartResponse removeCartItem(UUID cartItemId) {
        CartItem item = requireOwnedItem(cartItemId);
        Cart cart = item.getCart();
        cartItemRepository.delete(item);
        resetCartStoreWhenEmpty(cart);
        return toCartResponse(cart);
    }

    @Override
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public CartResponse clearCurrentCart() {
        Cart cart = currentCart();
        cartItemRepository.deleteByCartId(cart.getId());
        resetCartDraftState(cart);
        return toCartResponse(cart);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public CartDto getMyCart() {
        return toLegacyCartDto(currentCart());
    }

    @Override
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public CartDto addItem(AddCartItemRequest request) {
        addCartItem(request);
        return toLegacyCartDto(currentCart());
    }

    @Override
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public CartDto updateItem(UUID cartItemId, UpdateCartItemRequest request) {
        updateCartItem(cartItemId, request);
        return toLegacyCartDto(currentCart());
    }

    @Override
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public CartDto removeItem(UUID cartItemId) {
        removeCartItem(cartItemId);
        return toLegacyCartDto(currentCart());
    }

    @Override
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public CartDto clearCart() {
        clearCurrentCart();
        return toLegacyCartDto(currentCart());
    }

    @Override
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public CartDto applyCoupon(ApplyCouponRequest request) {
        Cart cart = currentCart();
        cart.setCouponCode(normalizeNullable(request.couponCode()));
        cartRepository.save(cart);
        return toLegacyCartDto(cart);
    }

    @Override
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public CartDto updateShippingDraft(CheckoutShippingRequest request) {
        Cart cart = currentCart();
        UserAccount user = currentUser();
        if (request.storeId() != null) {
            Store store = storeRepository.findById(request.storeId())
                    .orElseThrow(() -> new NotFoundException("STORE_NOT_FOUND", "Store not found"));
            cart.setStore(store);
        }
        if (request.addressId() != null) {
            Address address = addressRepository.findByIdAndUser(request.addressId(), user)
                    .orElseThrow(() -> new NotFoundException("ADDRESS_NOT_FOUND", "Address not found"));
            cart.setAddressId(address.getId());
        } else {
            cart.setAddressId(null);
        }
        cart.setFulfillmentMethod(request.fulfillmentMethod());
        cart.setShippingAddressSnapshot(normalizeNullable(request.shippingAddressSnapshot()));
        cartRepository.save(cart);
        return toLegacyCartDto(cart);
    }

    @Override
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public CartDto updatePaymentDraft(CheckoutPaymentRequest request) {
        Cart cart = currentCart();
        if (request.couponCode() != null) {
            cart.setCouponCode(normalizeNullable(request.couponCode()));
        }
        cart.setPaymentReference(normalizeNullable(request.paymentReference()));
        cart.setB2bInvoice(request.b2bInvoice());
        cart.setIdempotencyKey(normalizeNullable(request.idempotencyKey()));
        cartRepository.save(cart);
        return toLegacyCartDto(cart);
    }

    private Cart currentCart() {
        UserAccount user = currentUser();
        return cartRepository.findByUser(user).orElseGet(() -> {
            Cart created = new Cart();
            created.setUser(user);
            return cartRepository.save(created);
        });
    }

    private CartItem requireOwnedItem(UUID cartItemId) {
        return cartItemRepository.findOwnedItemById(cartItemId, currentUser().getId())
                .orElseThrow(() -> new NotFoundException("CART_ITEM_NOT_FOUND", "Cart item not found"));
    }

    private StoreProductReference requireActiveStoreProductReference(UUID referenceId) {
        return storeProductReferenceRepository.findByIdAndActiveTrue(referenceId)
                .orElseThrow(() -> new NotFoundException("STORE_PRODUCT_REFERENCE_NOT_FOUND", "Store product reference not found"));
    }

    private void ensureCartStoreCompatible(Cart cart, Store store) {
        if (store == null) {
            throw new BadRequestException("STORE_REQUIRED", "Store is required for cart items");
        }

        if (cart.getStore() == null) {
            cart.setStore(store);
            cartRepository.save(cart);
            return;
        }

        if (!cart.getStore().getId().equals(store.getId())) {
            throw new ForbiddenException("CART_STORE_CONFLICT", "Cart already contains items from another store");
        }
    }

    private CartItem createCartItem(Cart cart, StoreProductReference reference) {
        CartItem created = new CartItem();
        created.setCart(cart);
        created.setProduct(reference.getProduct());
        created.setStoreProductReference(reference);
        created.setQuantity(0);
        created.setUnitPrice(resolveUnitPrice(reference));
        return created;
    }

    private void validatePositiveQuantity(int quantity) {
        if (quantity < 1) {
            throw new BadRequestException("CART_QUANTITY_INVALID", "Quantity must be positive");
        }
    }

    private void validateAvailability(StoreProductReference reference, int quantity) {
        if (!reference.isActive()) {
            throw new BadRequestException("STORE_PRODUCT_INACTIVE", "Store product is inactive");
        }
        ProductInventory inventory = inventoryRepository
                .findByProductIdAndStoreId(reference.getProductId(), reference.getStoreId())
                .orElse(null);
        if (inventory != null && inventory.getStock() < quantity) {
            throw new BadRequestException("CART_ITEM_UNAVAILABLE", "Requested quantity is not available");
        }
    }

    private BigDecimal resolveUnitPrice(StoreProductReference reference) {
        return inventoryRepository.findByProductIdAndStoreId(reference.getProductId(), reference.getStoreId())
                .map(ProductInventory::getStorePrice)
                .orElseGet(() -> {
                    Product product = reference.getProduct();
                    return product.getBasePrice() == null ? BigDecimal.ZERO : product.getBasePrice();
                });
    }

    private void resetCartStoreWhenEmpty(Cart cart) {
        if (!cartItemRepository.findByCartIdOrderByCreatedAtAsc(cart.getId()).isEmpty()) {
            return;
        }
        resetCartDraftState(cart);
    }

    private void resetCartDraftState(Cart cart) {
        cart.setStore(null);
        cart.setFulfillmentMethod(null);
        cart.setShippingAddressSnapshot(null);
        cart.setAddressId(null);
        cart.setPaymentReference(null);
        cart.setCouponCode(null);
        cart.setB2bInvoice(false);
        cart.setIdempotencyKey(null);
        cartRepository.save(cart);
    }

    private List<CartItem> cartItems(Cart cart) {
        return cartItemRepository.findByCartIdOrderByCreatedAtAsc(cart.getId());
    }

    private CartResponse toCartResponse(Cart cart) {
        List<CartItem> items = cartItems(cart);
        List<CartItemResponse> itemResponses = items.stream()
                .map(this::toItemResponse)
                .toList();
        BigDecimal subtotal = calculateSubtotal(items);
        int totalQuantity = items.stream().mapToInt(CartItem::getQuantity).sum();

        return new CartResponse(
                cart.getId(),
                cart.getUserId(),
                cart.getStoreId(),
                itemResponses,
                totalQuantity,
                subtotal,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                subtotal,
                cart.getCreatedAt(),
                cart.getUpdatedAt()
        );
    }

    private CartItemResponse toItemResponse(CartItem item) {
        StoreProductReference reference = resolveStoreProductReference(item);
        Product product = resolveProduct(item, reference);
        Store store = resolveStore(item, reference);
        BigDecimal unitPrice = item.getUnitPrice() == null ? BigDecimal.ZERO : item.getUnitPrice();

        return new CartItemResponse(
                item.getId(),
                reference == null ? null : reference.getId(),
                product == null ? null : product.getId(),
                product == null ? null : product.getProductCode(),
                product == null ? null : product.getName(),
                store == null ? null : store.getId(),
                store == null ? null : store.getName(),
                item.getQuantity(),
                unitPrice,
                unitPrice.multiply(BigDecimal.valueOf(item.getQuantity())),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }

    private CartDto toLegacyCartDto(Cart cart) {
        List<CartItem> items = cartItems(cart);
        List<CartItemDto> lines = items.stream()
                .map(item -> {
                    Product product = resolveProduct(item, resolveStoreProductReference(item));
                    BigDecimal unitPrice = item.getUnitPrice() == null ? BigDecimal.ZERO : item.getUnitPrice();
                    return new CartItemDto(
                            item.getId(),
                            product == null ? null : product.getId(),
                            product == null ? null : product.getName(),
                            item.getQuantity(),
                            unitPrice,
                            unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()))
                    );
                })
                .toList();
        CartTotalsDto totals = pricingService.calculateTotals(items, cart.getStore(), cart.getCouponCode());
        return new CartDto(
                cart.getId(),
                cart.getStoreId(),
                cart.getAddressId(),
                lines,
                totals
        );
    }

    private BigDecimal calculateSubtotal(List<CartItem> items) {
        return items.stream()
                .map(item -> {
                    BigDecimal unitPrice = item.getUnitPrice() == null ? BigDecimal.ZERO : item.getUnitPrice();
                    return unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private StoreProductReference resolveStoreProductReference(CartItem item) {
        if (item.getStoreProductReference() != null) {
            return item.getStoreProductReference();
        }
        if (item.getCart() == null || item.getCart().getStore() == null || item.getProduct() == null) {
            return null;
        }
        return storeProductReferenceRepository.findByStoreIdAndProductId(item.getCart().getStoreId(), item.getProductId())
                .orElse(null);
    }

    private Product resolveProduct(CartItem item, StoreProductReference reference) {
        if (reference != null && reference.getProduct() != null) {
            return reference.getProduct();
        }
        return item.getProduct();
    }

    private Store resolveStore(CartItem item, StoreProductReference reference) {
        if (reference != null && reference.getStore() != null) {
            return reference.getStore();
        }
        return item.getCart() == null ? null : item.getCart().getStore();
    }

    private UserAccount currentUser() {
        return userAccountRepository.findByEmailIgnoreCase(SecurityUtils.currentEmail())
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
