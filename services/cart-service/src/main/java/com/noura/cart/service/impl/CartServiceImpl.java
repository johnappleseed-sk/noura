package com.noura.cart.service.impl;

import com.noura.cart.domain.entity.Cart;
import com.noura.cart.domain.entity.CartItem;
import com.noura.cart.domain.enums.CartItemValidationStatus;
import com.noura.cart.domain.enums.CartOwnerType;
import com.noura.cart.domain.enums.CartStatus;
import com.noura.cart.dto.cart.AddCartItemRequest;
import com.noura.cart.dto.cart.ApplyCouponRequest;
import com.noura.cart.dto.cart.CartItemResponse;
import com.noura.cart.dto.cart.CartResponse;
import com.noura.cart.dto.cart.CartTotalsResponse;
import com.noura.cart.dto.cart.MergeGuestCartRequest;
import com.noura.cart.dto.cart.UpdateCartItemQuantityRequest;
import com.noura.cart.exception.CartOperationException;
import com.noura.cart.exception.NotFoundException;
import com.noura.cart.integration.CatalogGateway;
import com.noura.cart.integration.InventoryGateway;
import com.noura.cart.integration.PromotionGateway;
import com.noura.cart.integration.PricingGateway;
import com.noura.cart.integration.model.InventorySnapshot;
import com.noura.cart.integration.model.PromotionEvaluationItem;
import com.noura.cart.integration.model.PromotionValidationSnapshot;
import com.noura.cart.integration.model.PricingSnapshot;
import com.noura.cart.integration.model.ProductSnapshot;
import com.noura.cart.repository.CartItemRepository;
import com.noura.cart.repository.CartRepository;
import com.noura.cart.service.CartService;
import com.noura.cart.service.model.CartContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Default implementation of {@link CartService}.
 *
 * <p>This service stores persistent carts and validates every mutation against
 * catalog, pricing, and inventory services.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CatalogGateway catalogGateway;
    private final PricingGateway pricingGateway;
    private final InventoryGateway inventoryGateway;
    private final PromotionGateway promotionGateway;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public CartResponse getCart(CartContext context) {
        Cart cart = findOrCreateCart(context, false);
        List<CartItem> items = cartItemRepository.findByCartIdOrderByCreatedAtAsc(cart.getId());
        return toResponse(cart, items);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public CartResponse addItem(CartContext context, AddCartItemRequest request) {
        Cart cart = findOrCreateCart(context, true);
        int incomingQuantity = request.quantity();

        CartItem item = cartItemRepository.findDeduplicatedItemForUpdate(
                        cart.getId(),
                        request.productId(),
                        request.variantId(),
                        request.storeId()
                )
                .orElseGet(() -> initializeNewItem(cart, request.productId(), request.variantId(), request.storeId()));

        int mergedQuantity = item.getQuantity() + incomingQuantity;
        if (mergedQuantity < 1) {
            throw new CartOperationException(HttpStatus.BAD_REQUEST, "CART_QUANTITY_INVALID", "Quantity must be positive");
        }

        ResolvedLine resolvedLine = resolveLine(request.productId(), request.storeId(), mergedQuantity, true);
        applyLineSnapshot(item, resolvedLine, mergedQuantity, context.actorId());
        cartItemRepository.save(item);

        if (cart.getStoreId() == null && request.storeId() != null) {
            cart.setStoreId(request.storeId());
        }
        recomputeTotals(cart, context.actorId(), false);

        List<CartItem> items = cartItemRepository.findByCartIdOrderByCreatedAtAsc(cart.getId());
        return toResponse(cart, items);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public CartResponse updateItemQuantity(CartContext context, UUID itemId, UpdateCartItemQuantityRequest request) {
        Cart cart = findOrCreateCart(context, true);
        CartItem item = cartItemRepository.findByIdAndCartId(itemId, cart.getId())
                .orElseThrow(() -> new NotFoundException("CART_ITEM_NOT_FOUND", "Cart item not found"));

        int replacementQuantity = request.quantity();
        if (replacementQuantity < 1) {
            throw new CartOperationException(HttpStatus.BAD_REQUEST, "CART_QUANTITY_INVALID", "Quantity must be positive");
        }

        ResolvedLine resolvedLine = resolveLine(item.getProductId(), item.getStoreId(), replacementQuantity, true);
        applyLineSnapshot(item, resolvedLine, replacementQuantity, context.actorId());
        cartItemRepository.save(item);

        recomputeTotals(cart, context.actorId(), false);
        List<CartItem> items = cartItemRepository.findByCartIdOrderByCreatedAtAsc(cart.getId());
        return toResponse(cart, items);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public CartResponse removeItem(CartContext context, UUID itemId) {
        Cart cart = findOrCreateCart(context, true);
        CartItem item = cartItemRepository.findByIdAndCartId(itemId, cart.getId())
                .orElseThrow(() -> new NotFoundException("CART_ITEM_NOT_FOUND", "Cart item not found"));
        cartItemRepository.delete(item);
        recomputeTotals(cart, context.actorId(), false);
        List<CartItem> items = cartItemRepository.findByCartIdOrderByCreatedAtAsc(cart.getId());
        return toResponse(cart, items);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public CartResponse clear(CartContext context) {
        Cart cart = findOrCreateCart(context, true);
        cartItemRepository.deleteByCartId(cart.getId());
        cart.setSubtotal(ZERO);
        cart.setDiscountAmount(ZERO);
        cart.setShippingAmount(ZERO);
        cart.setTotalAmount(ZERO);
        cart.setCouponCode(null);
        cart.setUpdatedBy(context.actorId());
        cartRepository.save(cart);
        return toResponse(cart, List.of());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public CartResponse mergeGuestCart(CartContext context, MergeGuestCartRequest request) {
        if (context.ownerType() != CartOwnerType.CUSTOMER) {
            throw new CartOperationException(
                    HttpStatus.BAD_REQUEST,
                    "CART_MERGE_REQUIRES_CUSTOMER",
                    "Guest-cart merge requires authenticated customer context"
            );
        }
        String sourceGuestToken = normalizeNullable(request.guestToken());
        if (sourceGuestToken == null) {
            throw new CartOperationException(HttpStatus.BAD_REQUEST, "GUEST_TOKEN_REQUIRED", "guestToken is required");
        }

        Cart target = findOrCreateCart(context, true);
        Optional<Cart> sourceOptional = cartRepository.findActiveGuestCartForUpdate(sourceGuestToken);
        if (sourceOptional.isEmpty()) {
            List<CartItem> targetItems = cartItemRepository.findByCartIdOrderByCreatedAtAsc(target.getId());
            return toResponse(target, targetItems);
        }

        Cart source = sourceOptional.get();
        if (source.getId().equals(target.getId())) {
            List<CartItem> targetItems = cartItemRepository.findByCartIdOrderByCreatedAtAsc(target.getId());
            return toResponse(target, targetItems);
        }

        List<CartItem> sourceItems = cartItemRepository.findByCartIdOrderByCreatedAtAsc(source.getId());
        for (CartItem sourceItem : sourceItems) {
            mergeLineIntoTarget(target, sourceItem, context.actorId());
        }

        source.setStatus(CartStatus.MERGED);
        source.setMergedIntoCartId(target.getId());
        source.setUpdatedBy(context.actorId());
        cartRepository.save(source);
        cartItemRepository.deleteByCartId(source.getId());

        recomputeTotals(target, context.actorId(), false);
        List<CartItem> targetItems = cartItemRepository.findByCartIdOrderByCreatedAtAsc(target.getId());
        return toResponse(target, targetItems);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public CartResponse refresh(CartContext context, boolean strict) {
        Cart cart = findOrCreateCart(context, true);
        List<CartItem> items = cartItemRepository.findByCartIdOrderByCreatedAtAsc(cart.getId());

        for (CartItem item : items) {
            ResolvedLine resolvedLine;
            try {
                resolvedLine = resolveLine(item.getProductId(), item.getStoreId(), item.getQuantity(), strict);
            } catch (CartOperationException ex) {
                if (strict) {
                    throw ex;
                }
                resolvedLine = ResolvedLine.unknown(
                        item.getProductId(),
                        item.getStoreId(),
                        "Validation failed due to temporary dependency issue"
                );
            }
            applyLineSnapshot(item, resolvedLine, item.getQuantity(), context.actorId());
            cartItemRepository.save(item);
        }

        recomputeTotals(cart, context.actorId(), strict);
        List<CartItem> refreshed = cartItemRepository.findByCartIdOrderByCreatedAtAsc(cart.getId());
        return toResponse(cart, refreshed);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public CartResponse applyCoupon(CartContext context, ApplyCouponRequest request) {
        Cart cart = findOrCreateCart(context, true);
        List<CartItem> items = cartItemRepository.findByCartIdOrderByCreatedAtAsc(cart.getId());
        if (items.isEmpty()) {
            throw new CartOperationException(
                    HttpStatus.CONFLICT,
                    "CART_EMPTY",
                    "Cannot apply coupon to an empty cart"
            );
        }

        cart.setCouponCode(normalizeNullable(request.couponCode()));
        recomputeTotals(cart, context.actorId(), true);
        List<CartItem> refreshed = cartItemRepository.findByCartIdOrderByCreatedAtAsc(cart.getId());
        return toResponse(cart, refreshed);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public CartResponse removeCoupon(CartContext context) {
        Cart cart = findOrCreateCart(context, true);
        cart.setCouponCode(null);
        recomputeTotals(cart, context.actorId(), false);
        List<CartItem> refreshed = cartItemRepository.findByCartIdOrderByCreatedAtAsc(cart.getId());
        return toResponse(cart, refreshed);
    }

    /**
     * Merges one source line into target cart and revalidates final quantity.
     *
     * @param target target cart aggregate
     * @param sourceItem source guest line item
     * @param actor actor identifier for audit fields
     */
    private void mergeLineIntoTarget(Cart target, CartItem sourceItem, String actor) {
        CartItem targetItem = cartItemRepository.findDeduplicatedItemForUpdate(
                        target.getId(),
                        sourceItem.getProductId(),
                        sourceItem.getVariantId(),
                        sourceItem.getStoreId()
                )
                .orElseGet(() -> initializeNewItem(
                        target,
                        sourceItem.getProductId(),
                        sourceItem.getVariantId(),
                        sourceItem.getStoreId()
                ));

        int mergedQuantity = targetItem.getQuantity() + sourceItem.getQuantity();
        ResolvedLine resolvedLine = resolveLine(sourceItem.getProductId(), sourceItem.getStoreId(), mergedQuantity, true);
        applyLineSnapshot(targetItem, resolvedLine, mergedQuantity, actor);
        cartItemRepository.save(targetItem);
    }

    /**
     * Initializes a new in-memory cart line item before persistence.
     *
     * @param cart target cart
     * @param productId product identifier
     * @param variantId optional variant identifier
     * @param storeId optional store/location identifier
     * @return initialized mutable line item
     */
    private CartItem initializeNewItem(Cart cart, UUID productId, UUID variantId, UUID storeId) {
        CartItem item = new CartItem();
        item.setCart(cart);
        item.setProductId(productId);
        item.setVariantId(variantId);
        item.setStoreId(storeId);
        item.setQuantity(0);
        item.setUnitPrice(ZERO);
        item.setLineTotal(ZERO);
        item.setCurrencyCode(cart.getCurrencyCode());
        item.setValidationStatus(CartItemValidationStatus.UNKNOWN);
        item.setValidationMessage("Line has not been validated yet");
        item.setAvailableQuantity(null);
        return item;
    }

    /**
     * Resolves downstream validation snapshot for a line mutation.
     *
     * @param productId product identifier
     * @param storeId optional store/location identifier
     * @param quantity target quantity
     * @param strict when true, invalid state throws operation exception
     * @return resolved line validation snapshot
     */
    private ResolvedLine resolveLine(UUID productId, UUID storeId, int quantity, boolean strict) {
        ProductSnapshot product = catalogGateway.findProduct(productId).orElse(null);
        if (product == null) {
            if (strict) {
                throw new NotFoundException("PRODUCT_NOT_FOUND", "Product does not exist in catalog");
            }
            return ResolvedLine.invalid(
                    productId,
                    storeId,
                    CartItemValidationStatus.PRODUCT_NOT_FOUND,
                    "Product does not exist in catalog"
            );
        }

        PricingSnapshot pricing = pricingGateway.resolvePrice(productId, storeId).orElse(null);
        if (pricing == null || pricing.unitPrice() == null) {
            if (strict) {
                throw new CartOperationException(HttpStatus.CONFLICT, "PRICE_UNAVAILABLE", "Price is not available for this product");
            }
            return ResolvedLine.invalid(
                    productId,
                    storeId,
                    CartItemValidationStatus.PRICE_UNAVAILABLE,
                    "Price is not available for this product",
                    product,
                    null,
                    null
            );
        }

        InventorySnapshot availability = inventoryGateway.resolveAvailability(productId, storeId);
        BigDecimal availableQuantity = normalize(availability.availableQuantity());
        BigDecimal requestedQuantity = BigDecimal.valueOf(quantity).setScale(4, RoundingMode.HALF_UP);
        if (!product.allowBackorder()) {
            if (availableQuantity.signum() <= 0) {
                if (strict) {
                    throw new CartOperationException(
                            HttpStatus.CONFLICT,
                            "OUT_OF_STOCK",
                            "Product is currently out of stock"
                    );
                }
                return ResolvedLine.invalid(
                        productId,
                        storeId,
                        CartItemValidationStatus.OUT_OF_STOCK,
                        "Product is currently out of stock",
                        product,
                        pricing,
                        availableQuantity
                );
            }
            if (availableQuantity.compareTo(requestedQuantity) < 0) {
                if (strict) {
                    throw new CartOperationException(
                            HttpStatus.CONFLICT,
                            "INSUFFICIENT_STOCK",
                            "Available stock is lower than requested quantity"
                    );
                }
                return ResolvedLine.invalid(
                        productId,
                        storeId,
                        CartItemValidationStatus.INSUFFICIENT_STOCK,
                        "Available stock is lower than requested quantity",
                        product,
                        pricing,
                        availableQuantity
                );
            }
        }

        return ResolvedLine.valid(productId, storeId, product, pricing, availableQuantity);
    }

    /**
     * Applies resolved validation and pricing snapshot to a mutable cart item.
     *
     * @param item target mutable item
     * @param resolvedLine resolved validation snapshot
     * @param quantity final quantity
     * @param actor actor identifier for audit field
     */
    private void applyLineSnapshot(CartItem item, ResolvedLine resolvedLine, int quantity, String actor) {
        item.setQuantity(quantity);
        if (resolvedLine.product() != null) {
            item.setProductNameSnapshot(resolvedLine.product().productName());
            item.setProductCodeSnapshot(resolvedLine.product().productCode());
            item.setSkuSnapshot(resolvedLine.product().sku());
        } else if (item.getProductNameSnapshot() == null) {
            item.setProductNameSnapshot("Unknown product");
        }

        if (resolvedLine.pricing() != null) {
            item.setUnitPrice(normalize(resolvedLine.pricing().unitPrice()));
            item.setCurrencyCode(normalizeCurrency(resolvedLine.pricing().currencyCode()));
        } else {
            item.setUnitPrice(ZERO);
        }

        item.setLineTotal(item.getUnitPrice().multiply(BigDecimal.valueOf(quantity)).setScale(4, RoundingMode.HALF_UP));
        item.setValidationStatus(resolvedLine.validationStatus());
        item.setValidationMessage(resolvedLine.validationMessage());
        item.setAvailableQuantity(normalizeNullable(resolvedLine.availableQuantity()));
        item.setUpdatedBy(actor);
        if (item.getCreatedBy() == null) {
            item.setCreatedBy(actor);
        }
    }

    /**
     * Recomputes cart totals from persisted line items and applied promotion state.
     *
     * @param cart target cart aggregate
     * @param actor actor identifier for audit field
     * @param strictPromotion when true, promotion-service failures reject the command
     */
    private void recomputeTotals(Cart cart, String actor, boolean strictPromotion) {
        List<CartItem> items = cartItemRepository.findByCartIdOrderByCreatedAtAsc(cart.getId());
        BigDecimal subtotal = items.stream()
                .map(CartItem::getLineTotal)
                .map(this::normalize)
                .reduce(ZERO, BigDecimal::add)
                .setScale(4, RoundingMode.HALF_UP);

        cart.setSubtotal(subtotal);

        String couponCode = normalizeNullable(cart.getCouponCode());
        BigDecimal discountAmount = ZERO;
        BigDecimal shippingAmount = ZERO;

        if (couponCode != null && !items.isEmpty() && subtotal.signum() > 0) {
            try {
                PromotionValidationSnapshot validation = promotionGateway.validateCoupon(
                        couponCode,
                        subtotal,
                        toPromotionItems(items),
                        currentCorrelationId()
                );
                if (!validation.valid() || !validation.eligible()) {
                    if (strictPromotion) {
                        throw new CartOperationException(
                                HttpStatus.BAD_REQUEST,
                                !validation.valid() ? "COUPON_INVALID" : "COUPON_INELIGIBLE",
                                resolveCouponFailureDetail(validation)
                        );
                    }
                    // Invalid or ineligible coupons are cleared on non-strict paths so cart totals stay deterministic.
                    cart.setCouponCode(null);
                } else if (validation.evaluation() != null) {
                    discountAmount = normalize(validation.evaluation().discountAmount()).min(subtotal);
                    shippingAmount = validation.evaluation().freeShipping() ? ZERO : shippingAmount;
                    cart.setCouponCode(couponCode);
                } else if (strictPromotion) {
                    throw new CartOperationException(
                            HttpStatus.BAD_GATEWAY,
                            "PROMOTION_SERVICE_INVALID_RESPONSE",
                            "Promotion service returned an incomplete coupon-evaluation response"
                    );
                } else {
                    cart.setCouponCode(null);
                }
            } catch (CartOperationException ex) {
                if (strictPromotion) {
                    throw ex;
                }
                log.warn("Coupon evaluation skipped for cart {} due to transient promotion failure: {}",
                        cart.getId(), ex.getCode());
                cart.setCouponCode(null);
                discountAmount = ZERO;
                shippingAmount = ZERO;
            }
        } else if (couponCode == null) {
            cart.setCouponCode(null);
        }

        BigDecimal total = subtotal.subtract(discountAmount).add(shippingAmount);
        if (total.signum() < 0) {
            total = ZERO;
        }

        cart.setDiscountAmount(discountAmount);
        cart.setShippingAmount(shippingAmount);
        cart.setTotalAmount(total);
        cart.setUpdatedBy(actor);
        if (cart.getCreatedBy() == null) {
            cart.setCreatedBy(actor);
        }
        cartRepository.save(cart);
    }

    /**
     * Maps cart line items into promotion-evaluation inputs.
     *
     * @param items cart line items
     * @return promotion item payloads
     */
    private List<PromotionEvaluationItem> toPromotionItems(List<CartItem> items) {
        return items.stream()
                .map(item -> new PromotionEvaluationItem(
                        item.getProductId(),
                        item.getVariantId(),
                        item.getQuantity(),
                        normalize(item.getUnitPrice())
                ))
                .toList();
    }

    /**
     * Resolves coupon failure detail from promotion validation payload.
     *
     * @param validation validation payload
     * @return human-readable error detail
     */
    private String resolveCouponFailureDetail(PromotionValidationSnapshot validation) {
        String reason = normalizeNullable(validation.reasonMessage());
        if (reason != null) {
            return reason;
        }
        String reasonCode = normalizeNullable(validation.reasonCode());
        if (reasonCode != null) {
            return "Coupon is not eligible (" + reasonCode + ")";
        }
        return "Coupon is not eligible for the current cart";
    }

    /**
     * Finds active cart for context and creates one when absent.
     *
     * @param context ownership context
     * @param lockForUpdate whether to acquire pessimistic lock
     * @return active cart aggregate
     */
    private Cart findOrCreateCart(CartContext context, boolean lockForUpdate) {
        Optional<Cart> existing = switch (context.ownerType()) {
            case CUSTOMER -> lockForUpdate
                    ? cartRepository.findActiveCustomerCartForUpdate(context.customerId())
                    : cartRepository.findByOwnerTypeAndCustomerIdAndStatus(
                    CartOwnerType.CUSTOMER,
                    context.customerId(),
                    CartStatus.ACTIVE
            );
            case GUEST -> lockForUpdate
                    ? cartRepository.findActiveGuestCartForUpdate(context.guestToken())
                    : cartRepository.findByOwnerTypeAndGuestTokenAndStatus(
                    CartOwnerType.GUEST,
                    context.guestToken(),
                    CartStatus.ACTIVE
            );
        };

        if (existing.isPresent()) {
            return existing.get();
        }
        return createCart(context);
    }

    /**
     * Creates and persists a new active cart aggregate.
     *
     * @param context ownership context
     * @return newly persisted cart
     */
    private Cart createCart(CartContext context) {
        Cart cart = new Cart();
        cart.setOwnerType(context.ownerType());
        cart.setStatus(CartStatus.ACTIVE);
        cart.setCurrencyCode("USD");
        cart.setSubtotal(ZERO);
        cart.setDiscountAmount(ZERO);
        cart.setShippingAmount(ZERO);
        cart.setTotalAmount(ZERO);
        cart.setCreatedBy(context.actorId());
        cart.setUpdatedBy(context.actorId());
        if (context.ownerType() == CartOwnerType.CUSTOMER) {
            cart.setCustomerId(context.customerId());
        } else {
            cart.setGuestToken(context.guestToken());
        }
        return cartRepository.save(cart);
    }

    /**
     * Maps cart aggregate and line items to API read model.
     *
     * @param cart cart aggregate
     * @param items line item list
     * @return cart response DTO
     */
    private CartResponse toResponse(Cart cart, List<CartItem> items) {
        List<CartItemResponse> itemResponses = items.stream()
                .map(this::toItemResponse)
                .toList();
        int itemCount = items.stream().mapToInt(CartItem::getQuantity).sum();
        CartTotalsResponse totals = new CartTotalsResponse(
                normalize(cart.getSubtotal()),
                normalize(cart.getDiscountAmount()),
                normalize(cart.getShippingAmount()),
                normalize(cart.getTotalAmount()),
                cart.getCouponCode()
        );

        return new CartResponse(
                cart.getId(),
                cart.getOwnerType().name(),
                cart.getCustomerId(),
                cart.getGuestToken(),
                normalizeCurrency(cart.getCurrencyCode()),
                cart.getStoreId(),
                cart.getAddressId(),
                itemResponses,
                totals,
                itemCount,
                cart.getUpdatedAt()
        );
    }

    /**
     * Maps one cart line item to API read model.
     *
     * @param item mutable item entity
     * @return line item DTO
     */
    private CartItemResponse toItemResponse(CartItem item) {
        return new CartItemResponse(
                item.getId(),
                item.getProductId(),
                item.getVariantId(),
                item.getStoreId(),
                item.getProductCodeSnapshot(),
                item.getProductNameSnapshot(),
                item.getSkuSnapshot(),
                item.getQuantity(),
                normalize(item.getUnitPrice()),
                normalize(item.getLineTotal()),
                normalizeNullable(item.getAvailableQuantity()),
                item.getValidationStatus(),
                item.getValidationMessage(),
                item.getUpdatedAt()
        );
    }

    /**
     * Normalizes nullable monetary quantity to scale 4.
     *
     * @param value input value
     * @return normalized value
     */
    private BigDecimal normalize(BigDecimal value) {
        return value == null ? ZERO : value.setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * Normalizes nullable quantity value while preserving nulls.
     *
     * @param value input value
     * @return normalized value, or null when absent
     */
    private BigDecimal normalizeNullable(BigDecimal value) {
        return value == null ? null : value.setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * Normalizes nullable string by trimming and converting blanks to null.
     *
     * @param value input value
     * @return normalized string or null
     */
    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * Reads current request correlation ID from MDC.
     *
     * @return correlation ID or {@code null}
     */
    private String currentCorrelationId() {
        return normalizeNullable(MDC.get("correlationId"));
    }

    /**
     * Normalizes nullable currency code.
     *
     * @param value input value
     * @return normalized currency code
     */
    private String normalizeCurrency(String value) {
        String normalized = normalizeNullable(value);
        return normalized == null ? "USD" : normalized.toUpperCase();
    }

    /**
     * Internal immutable line validation snapshot.
     *
     * @param productId product identifier
     * @param storeId optional store/location identifier
     * @param product product snapshot when available
     * @param pricing pricing snapshot when available
     * @param availableQuantity available quantity snapshot when available
     * @param validationStatus validation status
     * @param validationMessage validation detail message
     */
    private record ResolvedLine(
            UUID productId,
            UUID storeId,
            ProductSnapshot product,
            PricingSnapshot pricing,
            BigDecimal availableQuantity,
            CartItemValidationStatus validationStatus,
            String validationMessage
    ) {
        /**
         * Creates valid line snapshot.
         *
         * @param productId product identifier
         * @param storeId optional store scope
         * @param product resolved product snapshot
         * @param pricing resolved pricing snapshot
         * @param availableQuantity resolved available quantity
         * @return valid line snapshot
         */
        private static ResolvedLine valid(
                UUID productId,
                UUID storeId,
                ProductSnapshot product,
                PricingSnapshot pricing,
                BigDecimal availableQuantity
        ) {
            return new ResolvedLine(
                    productId,
                    storeId,
                    product,
                    pricing,
                    availableQuantity,
                    CartItemValidationStatus.VALID,
                    null
            );
        }

        /**
         * Creates invalid line snapshot without pricing/product context.
         *
         * @param productId product identifier
         * @param storeId optional store scope
         * @param status validation status
         * @param message validation message
         * @return invalid line snapshot
         */
        private static ResolvedLine invalid(
                UUID productId,
                UUID storeId,
                CartItemValidationStatus status,
                String message
        ) {
            return new ResolvedLine(productId, storeId, null, null, null, status, message);
        }

        /**
         * Creates invalid line snapshot with optional context.
         *
         * @param productId product identifier
         * @param storeId optional store scope
         * @param status validation status
         * @param message validation message
         * @param product product snapshot
         * @param pricing pricing snapshot
         * @param availableQuantity available quantity snapshot
         * @return invalid line snapshot
         */
        private static ResolvedLine invalid(
                UUID productId,
                UUID storeId,
                CartItemValidationStatus status,
                String message,
                ProductSnapshot product,
                PricingSnapshot pricing,
                BigDecimal availableQuantity
        ) {
            return new ResolvedLine(productId, storeId, product, pricing, availableQuantity, status, message);
        }

        /**
         * Creates UNKNOWN line snapshot for dependency failure fallback.
         *
         * @param productId product identifier
         * @param storeId optional store scope
         * @param message validation detail message
         * @return unknown line snapshot
         */
        private static ResolvedLine unknown(UUID productId, UUID storeId, String message) {
            return new ResolvedLine(
                    productId,
                    storeId,
                    null,
                    null,
                    null,
                    CartItemValidationStatus.UNKNOWN,
                    message
            );
        }
    }
}
