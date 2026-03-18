package com.noura.checkout.service.impl;

import com.noura.checkout.dto.checkout.CheckoutIssueResponse;
import com.noura.checkout.dto.checkout.CheckoutLineValidationResponse;
import com.noura.checkout.dto.checkout.CheckoutOrderSummaryResponse;
import com.noura.checkout.dto.checkout.CheckoutPaymentSummaryResponse;
import com.noura.checkout.dto.checkout.CheckoutPlaceOrderRequest;
import com.noura.checkout.dto.checkout.CheckoutPlaceOrderResponse;
import com.noura.checkout.dto.checkout.CheckoutPreviewRequest;
import com.noura.checkout.dto.checkout.CheckoutPreviewResponse;
import com.noura.checkout.dto.checkout.CheckoutTotalsResponse;
import com.noura.checkout.dto.checkout.CheckoutValidateRequest;
import com.noura.checkout.dto.checkout.CheckoutValidationResponse;
import com.noura.checkout.dto.checkout.ReservedStockResponse;
import com.noura.checkout.dto.checkout.ShippingAddressSnapshotDto;
import com.noura.checkout.exception.CheckoutOperationException;
import com.noura.checkout.integration.client.CartServiceClient;
import com.noura.checkout.integration.client.CustomerServiceClient;
import com.noura.checkout.integration.client.InventoryServiceClient;
import com.noura.checkout.integration.client.NotificationServiceClient;
import com.noura.checkout.integration.client.OrderServiceClient;
import com.noura.checkout.integration.client.PricingServiceClient;
import com.noura.checkout.service.CheckoutIdempotencyService;
import com.noura.checkout.service.CheckoutOrchestrationService;
import com.noura.checkout.service.PaymentGateway;
import com.noura.checkout.service.model.CheckoutRequestContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.stream.Collectors;

/**
 * Default implementation of checkout orchestration workflows.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CheckoutOrchestrationServiceImpl implements CheckoutOrchestrationService {

    private static final int IDEMPOTENCY_KEY_MAX_LENGTH = 128;
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);

    private final CartServiceClient cartServiceClient;
    private final CustomerServiceClient customerServiceClient;
    private final PricingServiceClient pricingServiceClient;
    private final InventoryServiceClient inventoryServiceClient;
    private final OrderServiceClient orderServiceClient;
    private final NotificationServiceClient notificationServiceClient;
    private final CheckoutIdempotencyService checkoutIdempotencyService;
    private final PaymentGateway paymentGateway;

    /**
     * {@inheritDoc}
     */
    @Override
    public CheckoutPreviewResponse preview(CheckoutRequestContext context, CheckoutPreviewRequest request) {
        PreparedCheckout prepared = prepareCheckout(
                context,
                request == null ? new CheckoutPreviewRequest(null, null, null) : request,
                false
        );
        return prepared.toPreviewResponse();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CheckoutValidationResponse validate(CheckoutRequestContext context, CheckoutValidateRequest request) {
        CheckoutValidateRequest safeRequest = request == null
                ? new CheckoutValidateRequest(null, null, null)
                : request;
        PreparedCheckout prepared = prepareCheckout(
                context,
                new CheckoutPreviewRequest(safeRequest.storeId(), safeRequest.addressId(), safeRequest.couponCode()),
                true
        );
        return new CheckoutValidationResponse(
                prepared.valid(),
                prepared.issues(),
                prepared.toPreviewResponse(),
                Instant.now()
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CheckoutPlaceOrderResponse placeOrder(
            CheckoutRequestContext context,
            CheckoutPlaceOrderRequest request,
            String idempotencyKeyHeader
    ) {
        requireAuthenticatedCustomer(context);
        CheckoutPlaceOrderRequest safeRequest = request == null
                ? new CheckoutPlaceOrderRequest(null, null, null, null, null, null, null, null)
                : request;

        String idempotencyKey = normalizeIdempotencyKey(idempotencyKeyHeader, safeRequest.idempotencyKey());
        UUID idempotencyRecordId = null;

        if (idempotencyKey != null) {
            Optional<CheckoutPlaceOrderResponse> replay = checkoutIdempotencyService.tryReplay(context.subject(), idempotencyKey);
            if (replay.isPresent()) {
                return replay.get();
            }
            try {
                idempotencyRecordId = checkoutIdempotencyService.beginProcessing(
                        context.subject(),
                        idempotencyKey,
                        safeRequest,
                        context.actorId()
                );
            } catch (CheckoutOperationException ex) {
                if ("IDEMPOTENCY_ALREADY_COMPLETED".equals(ex.getCode())) {
                    return checkoutIdempotencyService.tryReplay(context.subject(), idempotencyKey)
                            .orElseThrow(() -> ex);
                }
                throw ex;
            }
        }

        try {
            PreparedCheckout prepared = prepareCheckout(
                    context,
                    new CheckoutPreviewRequest(safeRequest.storeId(), safeRequest.addressId(), safeRequest.couponCode()),
                    true
            );
            if (!prepared.valid()) {
                throw new CheckoutOperationException(
                        HttpStatus.CONFLICT,
                        "CHECKOUT_VALIDATION_FAILED",
                        resolveValidationFailureMessage(prepared.issues())
                );
            }

            String checkoutReference = idempotencyKey != null ? idempotencyKey : "checkout-" + UUID.randomUUID();
            log.info("Starting checkout placement for customer {} with reference {}",
                    context.subject(), checkoutReference);

            List<InventoryServiceClient.ReservationResult> reservations = reserveStock(prepared, context, checkoutReference);
            log.info("Reserved {} stock movement(s) for checkout reference {}",
                    reservations.size(), checkoutReference);

            OrderServiceClient.OrderPayload orderPayload;
            try {
                orderPayload = orderServiceClient.createOrder(
                        context.subject(),
                        context.authorizationHeader(),
                        context.correlationId(),
                        buildCreateOrderPayload(
                                context,
                                safeRequest,
                                prepared,
                                idempotencyKey,
                                checkoutReference
                        )
                );
            } catch (RuntimeException ex) {
                releaseReservations(reservations, context, checkoutReference);
                throw ex;
            }
            log.info("Created order {} with initial status {} for checkout reference {}",
                    orderPayload.id(), orderPayload.status(), checkoutReference);

            CheckoutPaymentSummaryResponse payment;
            try {
                payment = paymentGateway.createAndConfirmPayment(
                        context,
                        safeRequest,
                        orderPayload.id(),
                        prepared.totals().currencyCode(),
                        prepared.totals().totalAmount(),
                        idempotencyKey
                );
            } catch (RuntimeException ex) {
                releaseReservations(reservations, context, checkoutReference);
                cancelOrderAfterPaymentFailure(orderPayload.id(), context, "PAYMENT_GATEWAY_FAILED", "Payment confirmation failed during checkout");
                throw ex;
            }

            if (!isPaymentSuccessful(payment)) {
                releaseReservations(reservations, context, checkoutReference);
                cancelOrderAfterPaymentFailure(
                        orderPayload.id(),
                        context,
                        "PAYMENT_NOT_COMPLETED",
                        "Payment remained in non-complete status " + payment.status()
                );
                throw new CheckoutOperationException(
                        HttpStatus.CONFLICT,
                        "PAYMENT_NOT_COMPLETED",
                        "Payment could not be completed for this checkout"
                );
            }

            OrderServiceClient.OrderPayload finalizedOrder = orderServiceClient.updateOrderStatusInternal(
                    orderPayload.id(),
                    context.correlationId(),
                    new OrderServiceClient.UpdateOrderStatusPayload(
                            "PAID",
                            "NONE",
                            "PAYMENT_CONFIRMED",
                            buildOrderFinalizationNote(payment)
                    )
            );
            log.info("Finalized order {} as {} after payment {} with status {}",
                    finalizedOrder.id(), finalizedOrder.status(), payment.paymentReference(), payment.status());

            clearCartBestEffort(context);
            dispatchOrderPlacedNotification(context, finalizedOrder);

            CheckoutPlaceOrderResponse response = toPlaceOrderResponse(
                    finalizedOrder,
                    payment,
                    reservations,
                    idempotencyKey,
                    false
            );
            if (idempotencyRecordId != null) {
                checkoutIdempotencyService.markSuccess(idempotencyRecordId, response, context.actorId());
            }
            return response;
        } catch (CheckoutOperationException ex) {
            markIdempotencyFailure(idempotencyRecordId, context.actorId(), ex.getCode(), ex.getMessage());
            throw ex;
        } catch (RuntimeException ex) {
            markIdempotencyFailure(idempotencyRecordId, context.actorId(), "CHECKOUT_PLACE_ORDER_FAILED", ex.getMessage());
            throw new CheckoutOperationException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "CHECKOUT_PLACE_ORDER_FAILED",
                    "Unable to place order due to an unexpected error"
            );
        }
    }

    /**
     * Builds a normalized checkout snapshot from cart/pricing/inventory/customer services.
     *
     * @param context checkout request context
     * @param request preview-like request payload
     * @param requireAddress whether address is mandatory for this flow
     * @return prepared checkout snapshot
     */
    private PreparedCheckout prepareCheckout(
            CheckoutRequestContext context,
            CheckoutPreviewRequest request,
            boolean requireAddress
    ) {
        requireAuthenticatedCustomer(context);

        CartServiceClient.CartPayload cart = cartServiceClient.getActiveCart(
                context.subject(),
                context.authorizationHeader(),
                context.correlationId()
        );

        if (cart.items() == null || cart.items().isEmpty()) {
            throw new CheckoutOperationException(HttpStatus.CONFLICT, "CART_EMPTY", "Cart is empty");
        }

        UUID resolvedStoreId = resolveStoreId(cart, request.storeId());
        UUID resolvedAddressId = request.addressId() != null ? request.addressId() : cart.addressId();

        List<CheckoutIssueResponse> issues = new ArrayList<>();
        if (resolvedStoreId == null) {
            issues.add(new CheckoutIssueResponse(
                    "CHECKOUT_STORE_REQUIRED",
                    "A store/location is required to validate inventory",
                    null,
                    null
            ));
        }

        CustomerServiceClient.AddressPayload address = null;
        if (resolvedAddressId != null) {
            address = customerServiceClient.getAddress(
                    context.subject(),
                    resolvedAddressId,
                    context.authorizationHeader(),
                    context.correlationId()
            );
        } else if (requireAddress) {
            issues.add(new CheckoutIssueResponse(
                    "CHECKOUT_ADDRESS_REQUIRED",
                    "A shipping address is required before placing an order",
                    null,
                    null
            ));
        }

        List<CheckoutLineValidationResponse> lineResponses = new ArrayList<>();
        for (CartServiceClient.CartItemPayload item : cart.items()) {
            lineResponses.add(validateLine(item, resolvedStoreId, context.correlationId(), issues));
        }

        boolean valid = issues.isEmpty() && lineResponses.stream().allMatch(CheckoutLineValidationResponse::valid);
        CheckoutTotalsResponse totals = resolveTotals(cart, lineResponses);
        ShippingAddressSnapshotDto shippingAddress = toShippingSnapshot(address);
        return new PreparedCheckout(
                cart,
                context.subject(),
                resolvedStoreId,
                resolvedAddressId,
                address,
                lineResponses,
                issues,
                totals,
                valid,
                Instant.now()
        );
    }

    /**
     * Validates one cart line against pricing and inventory dependencies.
     *
     * @param item cart line payload
     * @param storeId resolved location identifier
     * @param correlationId correlation ID
     * @param issues mutable global issue list
     * @return line validation response
     */
    private CheckoutLineValidationResponse validateLine(
            CartServiceClient.CartItemPayload item,
            UUID storeId,
            String correlationId,
            List<CheckoutIssueResponse> issues
    ) {
        int quantity = item.quantity() == null ? 0 : item.quantity();
        BigDecimal fallbackUnitPrice = normalizeMoney(item.unitPrice());
        BigDecimal resolvedUnitPrice = fallbackUnitPrice;
        BigDecimal available = ZERO;

        String issueCode = null;
        String issueMessage = null;
        boolean valid = true;

        if (quantity <= 0) {
            valid = false;
            issueCode = "INVALID_QUANTITY";
            issueMessage = "Cart line quantity must be greater than zero";
        }

        if (valid && item.validationStatus() != null && !"VALID".equalsIgnoreCase(item.validationStatus())) {
            valid = false;
            issueCode = "CART_LINE_INVALID";
            issueMessage = item.validationMessage() == null || item.validationMessage().isBlank()
                    ? "Cart line is no longer valid"
                    : item.validationMessage().trim();
        }

        if (valid) {
            Optional<PricingServiceClient.PricePayload> pricePayload = pricingServiceClient.resolvePrice(
                    item.productId(),
                    storeId,
                    correlationId
            );
            if (pricePayload.isEmpty() || pricePayload.get().effectivePrice() == null) {
                valid = false;
                issueCode = "PRICE_UNAVAILABLE";
                issueMessage = "Effective price is unavailable for one or more cart lines";
            } else {
                resolvedUnitPrice = normalizeMoney(pricePayload.get().effectivePrice());
            }
        }

        if (valid) {
            if (storeId == null) {
                valid = false;
                issueCode = "CHECKOUT_STORE_REQUIRED";
                issueMessage = "Store/location is required for stock validation";
            } else {
                available = inventoryServiceClient.resolveAvailable(item.productId(), storeId, correlationId);
                if (available.compareTo(BigDecimal.valueOf(quantity).setScale(4, RoundingMode.HALF_UP)) < 0) {
                    valid = false;
                    issueCode = "INSUFFICIENT_STOCK";
                    issueMessage = "Available stock is below requested quantity";
                }
            }
        }

        BigDecimal lineTotal = resolvedUnitPrice.multiply(BigDecimal.valueOf(Math.max(quantity, 0L)))
                .setScale(4, RoundingMode.HALF_UP);

        if (!valid) {
            issues.add(new CheckoutIssueResponse(
                    issueCode,
                    issueMessage,
                    item.productId(),
                    item.id()
            ));
        }

        return new CheckoutLineValidationResponse(
                item.id(),
                item.productId(),
                item.variantId(),
                trimToEmpty(item.productName()),
                trimToEmpty(item.sku()),
                quantity,
                resolvedUnitPrice,
                lineTotal,
                available,
                storeId,
                valid,
                issueCode,
                issueMessage
        );
    }

    /**
     * Resolves checkout totals from validated line payloads and cart-level placeholders.
     *
     * @param cart cart payload
     * @param lines line validation responses
     * @return totals response
     */
    private CheckoutTotalsResponse resolveTotals(
            CartServiceClient.CartPayload cart,
            List<CheckoutLineValidationResponse> lines
    ) {
        BigDecimal subtotal = lines.stream()
                .map(CheckoutLineValidationResponse::lineTotal)
                .filter(Objects::nonNull)
                .map(this::normalizeMoney)
                .reduce(ZERO, BigDecimal::add);

        BigDecimal discountAmount = normalizeMoney(cart.totals() == null ? null : cart.totals().discountAmount());
        BigDecimal shippingAmount = normalizeMoney(cart.totals() == null ? null : cart.totals().shippingAmount());
        BigDecimal taxAmount = ZERO;

        BigDecimal total = subtotal.subtract(discountAmount).add(shippingAmount).add(taxAmount);
        if (total.signum() < 0) {
            total = ZERO;
        }
        String currencyCode = normalizeCurrencyCode(cart.currencyCode());
        return new CheckoutTotalsResponse(subtotal, discountAmount, shippingAmount, taxAmount, total, currencyCode);
    }

    /**
     * Reserves stock for all validated lines and compensates already reserved lines on partial failure.
     *
     * @param prepared prepared checkout snapshot
     * @param context request context
     * @param checkoutReference checkout reference
     * @return reservation result list
     */
    private List<InventoryServiceClient.ReservationResult> reserveStock(
            PreparedCheckout prepared,
            CheckoutRequestContext context,
            String checkoutReference
    ) {
        List<InventoryServiceClient.ReservationResult> reservations = new ArrayList<>();
        for (CheckoutLineValidationResponse line : prepared.lines()) {
            BigDecimal quantity = BigDecimal.valueOf(line.quantity()).setScale(4, RoundingMode.HALF_UP);
            try {
                InventoryServiceClient.ReservationResult reserved = inventoryServiceClient.reserve(
                        line.productId(),
                        prepared.storeId(),
                        quantity,
                        context.actorId(),
                        checkoutReference,
                        context.correlationId()
                );
                reservations.add(reserved);
            } catch (RuntimeException ex) {
                releaseReservations(reservations, context, checkoutReference);
                throw ex;
            }
        }
        return reservations;
    }

    /**
     * Releases already created reservations as rollback compensation.
     *
     * @param reservations reservations to release
     * @param context request context
     * @param checkoutReference checkout reference
     */
    private void releaseReservations(
            List<InventoryServiceClient.ReservationResult> reservations,
            CheckoutRequestContext context,
            String checkoutReference
    ) {
        for (InventoryServiceClient.ReservationResult reservation : reservations) {
            try {
                inventoryServiceClient.release(
                        reservation.productId(),
                        reservation.locationId(),
                        reservation.quantity(),
                        context.actorId(),
                        checkoutReference,
                        context.correlationId()
                );
            } catch (RuntimeException ex) {
                log.warn("Reservation rollback failed for product {}: {}", reservation.productId(), ex.getMessage());
            }
        }
    }

    /**
     * Builds create-order payload from validated checkout snapshot.
     *
     * @param context request context
     * @param request place-order request
     * @param prepared prepared checkout snapshot
     * @param idempotencyKey idempotency key
     * @param checkoutReference checkout reference
     * @return create-order payload
     */
    private OrderServiceClient.CreateOrderPayload buildCreateOrderPayload(
            CheckoutRequestContext context,
            CheckoutPlaceOrderRequest request,
            PreparedCheckout prepared,
            String idempotencyKey,
            String checkoutReference
    ) {
        List<OrderServiceClient.CreateOrderItemPayload> items = prepared.lines().stream()
                .map(line -> new OrderServiceClient.CreateOrderItemPayload(
                        line.productId(),
                        line.variantId(),
                        trimToNull(line.sku()),
                        line.productName(),
                        null,
                        line.quantity(),
                        line.unitPrice(),
                        line.lineTotal(),
                        Map.of(
                                "cartItemId", line.lineItemId(),
                                "validated", line.valid(),
                                "storeId", line.storeId()
                        )
                ))
                .toList();

        Map<String, Object> checkoutContext = new LinkedHashMap<>();
        checkoutContext.put("source", "checkout-service");
        checkoutContext.put("checkoutReference", checkoutReference);
        checkoutContext.put("cartId", prepared.cart().cartId());
        checkoutContext.put("validatedAt", Instant.now());
        checkoutContext.put("correlationId", context.correlationId());
        checkoutContext.put("paymentMethod", request.paymentMethod() == null ? "CREDIT_CARD" : request.paymentMethod().name());
        checkoutContext.put("paymentProvider", trimToNull(request.paymentProvider()) == null ? "mock" : trimToNull(request.paymentProvider()));
        checkoutContext.put("paymentProviderReference", trimToNull(request.paymentProviderReference()));
        checkoutContext.put("paymentAutoCapture", request.paymentAutoCapture() == null || request.paymentAutoCapture());

        OrderServiceClient.AddressSnapshotPayload shippingAddress = toOrderAddressSnapshot(prepared.address());

        return new OrderServiceClient.CreateOrderPayload(
                context.subject(),
                prepared.storeId(),
                prepared.addressId(),
                prepared.totals().currencyCode(),
                null,
                trimToNull(request.couponCode()) != null
                        ? trimToNull(request.couponCode())
                        : trimToNull(prepared.cart().totals() == null ? null : prepared.cart().totals().couponCode()),
                shippingAddress,
                shippingAddress,
                buildShippingAddressSnapshot(prepared.address()),
                checkoutContext,
                prepared.totals().subtotal(),
                prepared.totals().discountAmount(),
                prepared.totals().shippingAmount(),
                prepared.totals().taxAmount(),
                prepared.totals().totalAmount(),
                false,
                idempotencyKey,
                items
        );
    }

    /**
     * Converts optional address payload into order-service address snapshot payload.
     *
     * @param address address payload
     * @return address snapshot payload or {@code null}
     */
    private OrderServiceClient.AddressSnapshotPayload toOrderAddressSnapshot(CustomerServiceClient.AddressPayload address) {
        if (address == null) {
            return null;
        }
        return new OrderServiceClient.AddressSnapshotPayload(
                trimToNull(address.fullName()),
                trimToNull(address.phone()),
                trimToNull(address.line1()),
                trimToNull(address.line2()),
                trimToNull(address.district()),
                trimToNull(address.city()),
                trimToNull(address.stateProvince()),
                trimToNull(address.postalCode()),
                trimToNull(address.countryCode())
        );
    }

    /**
     * Builds shipping address snapshot string.
     *
     * @param address address payload
     * @return shipping snapshot string
     */
    private String buildShippingAddressSnapshot(CustomerServiceClient.AddressPayload address) {
        if (address == null) {
            return "Store pickup requested";
        }
        if (address.formattedAddress() != null && !address.formattedAddress().isBlank()) {
            return address.formattedAddress().trim();
        }
        return Stream.of(
                        trimToNull(address.fullName()),
                        trimToNull(address.line1()),
                        trimToNull(address.line2()),
                        trimToNull(address.district()),
                        trimToNull(address.city()),
                        trimToNull(address.stateProvince()),
                        trimToNull(address.postalCode()),
                        trimToNull(address.countryCode())
                )
                .filter(Objects::nonNull)
                .collect(Collectors.joining(", "));
    }

    /**
     * Maps internal prepared checkout snapshot into shipping address response DTO.
     *
     * @param address address payload
     * @return shipping snapshot DTO or {@code null}
     */
    private ShippingAddressSnapshotDto toShippingSnapshot(CustomerServiceClient.AddressPayload address) {
        if (address == null) {
            return null;
        }
        return new ShippingAddressSnapshotDto(
                trimToNull(address.fullName()),
                trimToNull(address.phone()),
                trimToNull(address.line1()),
                trimToNull(address.line2()),
                trimToNull(address.district()),
                trimToNull(address.city()),
                trimToNull(address.stateProvince()),
                trimToNull(address.postalCode()),
                trimToNull(address.countryCode()),
                trimToNull(address.formattedAddress()),
                trimToNull(address.validationStatus())
        );
    }

    /**
     * Maps order and reservation models into place-order response DTO.
     *
     * @param order order payload
     * @param payment payment payload
     * @param reservations reservation results
     * @param idempotencyKey idempotency key
     * @param replayed replay marker
     * @return place-order response
     */
    private CheckoutPlaceOrderResponse toPlaceOrderResponse(
            OrderServiceClient.OrderPayload order,
            CheckoutPaymentSummaryResponse payment,
            List<InventoryServiceClient.ReservationResult> reservations,
            String idempotencyKey,
            boolean replayed
    ) {
        List<ReservedStockResponse> reserved = reservations.stream()
                .map(entry -> new ReservedStockResponse(
                        entry.productId(),
                        entry.locationId(),
                        entry.quantity(),
                        entry.movementId()
                ))
                .toList();

        Instant placedAt = order.placedAt() != null ? order.placedAt() : order.createdAt();
        CheckoutOrderSummaryResponse orderSummary = new CheckoutOrderSummaryResponse(
                order.id(),
                resolveOrderNumber(order),
                trimToEmpty(order.status()),
                normalizeMoney(order.totalAmount()),
                normalizeCurrencyCode(order.currencyCode()),
                placedAt
        );
        return new CheckoutPlaceOrderResponse(
                orderSummary,
                payment,
                reserved,
                idempotencyKey,
                replayed,
                placedAt,
                "Order placed successfully"
        );
    }

    /**
     * Determines whether checkout can finalize the order after synchronous payment handling.
     *
     * @param payment payment summary
     * @return {@code true} when payment reached a usable success state
     */
    private boolean isPaymentSuccessful(CheckoutPaymentSummaryResponse payment) {
        if (payment == null || payment.status() == null) {
            return false;
        }
        return "CAPTURED".equalsIgnoreCase(payment.status()) || "AUTHORIZED".equalsIgnoreCase(payment.status());
    }

    /**
     * Cancels an order after payment failure without masking the original checkout error.
     *
     * @param orderId order identifier
     * @param context request context
     * @param reason transition reason
     * @param note operator note
     */
    private void cancelOrderAfterPaymentFailure(
            UUID orderId,
            CheckoutRequestContext context,
            String reason,
            String note
    ) {
        try {
            orderServiceClient.updateOrderStatusInternal(
                    orderId,
                    context.correlationId(),
                    new OrderServiceClient.UpdateOrderStatusPayload(
                            "CANCELLED",
                            "NONE",
                            reason,
                            note
                    )
            );
        } catch (RuntimeException ex) {
            log.warn("Failed to cancel order {} after checkout payment failure: {}", orderId, ex.getMessage());
        }
    }

    /**
     * Clears the current cart after a successful checkout without masking order completion.
     *
     * @param context request context
     */
    private void clearCartBestEffort(CheckoutRequestContext context) {
        try {
            cartServiceClient.clearCart(context.subject(), context.authorizationHeader(), context.correlationId());
        } catch (RuntimeException ex) {
            log.warn("Cart clear skipped after successful checkout for customer {}: {}", context.subject(), ex.getMessage());
        }
    }

    /**
     * Dispatches order placed notifications using the internal customer UUID when available.
     *
     * @param context request context
     * @param order finalized order payload
     */
    private void dispatchOrderPlacedNotification(
            CheckoutRequestContext context,
            OrderServiceClient.OrderPayload order
    ) {
        try {
            CustomerServiceClient.CustomerLookupPayload customer = customerServiceClient.lookupByExternalSubject(
                    context.subject(),
                    context.correlationId()
            );
            notificationServiceClient.sendOrderPlacedNotification(
                    customer.id(),
                    resolveOrderNumber(order),
                    context.correlationId()
            );
        } catch (RuntimeException ex) {
            log.warn("Order notification lookup skipped for customer {} and order {}: {}",
                    context.subject(), order.id(), ex.getMessage());
        }
    }

    /**
     * Builds a consistent audit note for order finalization.
     *
     * @param payment payment summary
     * @return transition note
     */
    private String buildOrderFinalizationNote(CheckoutPaymentSummaryResponse payment) {
        String reference = payment.paymentReference() == null ? "unknown" : payment.paymentReference();
        return "Checkout confirmed payment " + reference + " with status " + payment.status();
    }

    /**
     * Resolves order number fallback when order-service response omits business number.
     *
     * @param order order payload
     * @return order number-like value
     */
    private String resolveOrderNumber(OrderServiceClient.OrderPayload order) {
        if (order.orderNumber() != null && !order.orderNumber().isBlank()) {
            return order.orderNumber().trim();
        }
        return order.id() == null ? "ORDER" : "ORD-" + order.id().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Resolves store/location ID from request/cart/item fallback order.
     *
     * @param cart cart payload
     * @param requestedStoreId optional request store ID
     * @return resolved store ID or {@code null}
     */
    private UUID resolveStoreId(CartServiceClient.CartPayload cart, UUID requestedStoreId) {
        if (requestedStoreId != null) {
            return requestedStoreId;
        }
        if (cart.storeId() != null) {
            return cart.storeId();
        }
        Set<UUID> lineStores = cart.items().stream()
                .map(CartServiceClient.CartItemPayload::storeId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (lineStores.size() == 1) {
            return lineStores.iterator().next();
        }
        return null;
    }

    /**
     * Ensures current flow has authenticated customer context.
     *
     * @param context request context
     */
    private void requireAuthenticatedCustomer(CheckoutRequestContext context) {
        if (context == null || !context.hasSubject()) {
            throw new CheckoutOperationException(
                    HttpStatus.UNAUTHORIZED,
                    "AUTH_SUBJECT_REQUIRED",
                    "Authenticated customer identity is required"
            );
        }
    }

    /**
     * Resolves and validates idempotency key from request header/body.
     *
     * @param headerValue idempotency key header value
     * @param bodyValue idempotency key body value
     * @return normalized idempotency key or {@code null}
     */
    private String normalizeIdempotencyKey(String headerValue, String bodyValue) {
        String raw = trimToNull(headerValue);
        if (raw == null) {
            raw = trimToNull(bodyValue);
        }
        if (raw == null) {
            return null;
        }
        if (raw.length() > IDEMPOTENCY_KEY_MAX_LENGTH) {
            throw new CheckoutOperationException(
                    HttpStatus.BAD_REQUEST,
                    "IDEMPOTENCY_KEY_INVALID",
                    "Idempotency key must be <= 128 characters"
            );
        }
        return raw;
    }

    /**
     * Builds one human-readable validation failure message.
     *
     * @param issues checkout issues
     * @return failure message
     */
    private String resolveValidationFailureMessage(List<CheckoutIssueResponse> issues) {
        if (issues == null || issues.isEmpty()) {
            return "Checkout validation failed";
        }
        String first = issues.getFirst().detail();
        if (issues.size() == 1) {
            return first;
        }
        return first + " (+" + (issues.size() - 1) + " additional issue(s))";
    }

    /**
     * Writes idempotency failure status when idempotency is enabled.
     *
     * @param recordId idempotency record identifier
     * @param actor actor identifier
     * @param code failure code
     * @param message failure message
     */
    private void markIdempotencyFailure(UUID recordId, String actor, String code, String message) {
        if (recordId == null) {
            return;
        }
        checkoutIdempotencyService.markFailure(recordId, code, message, actor);
    }

    /**
     * Normalizes money to scale 4.
     *
     * @param value source value
     * @return normalized money value
     */
    private BigDecimal normalizeMoney(BigDecimal value) {
        if (value == null) {
            return ZERO;
        }
        return value.setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * Normalizes currency code with fallback.
     *
     * @param value source currency
     * @return normalized currency code
     */
    private String normalizeCurrencyCode(String value) {
        if (value == null || value.isBlank()) {
            return "USD";
        }
        String normalized = value.trim().toUpperCase();
        return normalized.length() > 8 ? normalized.substring(0, 8) : normalized;
    }

    /**
     * Trims source text and normalizes blanks to {@code null}.
     *
     * @param value source text
     * @return normalized value or {@code null}
     */
    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * Trims source text and normalizes null to empty string.
     *
     * @param value source text
     * @return normalized text
     */
    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * Internal prepared checkout snapshot for orchestration decisions.
     *
     * @param cart cart payload
     * @param customerRef customer reference
     * @param storeId resolved store identifier
     * @param addressId resolved address identifier
     * @param address resolved address payload
     * @param lines line validation responses
     * @param issues global issues
     * @param totals totals summary
     * @param valid validity marker
     * @param preparedAt prepared timestamp
     */
    private record PreparedCheckout(
            CartServiceClient.CartPayload cart,
            String customerRef,
            UUID storeId,
            UUID addressId,
            CustomerServiceClient.AddressPayload address,
            List<CheckoutLineValidationResponse> lines,
            List<CheckoutIssueResponse> issues,
            CheckoutTotalsResponse totals,
            boolean valid,
            Instant preparedAt
    ) {

        /**
         * Converts prepared snapshot to preview response DTO.
         *
         * @return preview response
         */
        private CheckoutPreviewResponse toPreviewResponse() {
            ShippingAddressSnapshotDto shippingAddress = address == null
                    ? null
                    : new ShippingAddressSnapshotDto(
                    address.fullName(),
                    address.phone(),
                    address.line1(),
                    address.line2(),
                    address.district(),
                    address.city(),
                    address.stateProvince(),
                    address.postalCode(),
                    address.countryCode(),
                    address.formattedAddress(),
                    address.validationStatus()
            );
            return new CheckoutPreviewResponse(
                    cart.cartId(),
                    customerRef,
                    storeId,
                    addressId,
                    valid,
                    lines,
                    issues,
                    totals,
                    shippingAddress,
                    preparedAt
            );
        }
    }
}
