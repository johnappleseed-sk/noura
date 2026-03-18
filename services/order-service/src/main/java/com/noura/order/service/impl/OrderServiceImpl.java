package com.noura.order.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.noura.order.domain.entity.OrderItemRecord;
import com.noura.order.domain.entity.OrderRecord;
import com.noura.order.domain.entity.OrderStatusHistory;
import com.noura.order.domain.enums.OrderStatus;
import com.noura.order.domain.enums.RefundStatus;
import com.noura.order.dto.order.CreateOrderItemRequest;
import com.noura.order.dto.order.CreateOrderRequest;
import com.noura.order.dto.order.OrderItemResponse;
import com.noura.order.dto.order.OrderResponse;
import com.noura.order.dto.order.OrderStatusEventResponse;
import com.noura.order.dto.order.UpdateOrderStatusRequest;
import com.noura.order.exception.NotFoundException;
import com.noura.order.exception.OrderOperationException;
import com.noura.order.repository.OrderItemRecordRepository;
import com.noura.order.repository.OrderRecordRepository;
import com.noura.order.repository.OrderStatusHistoryRepository;
import com.noura.order.service.OrderService;
import com.noura.order.service.model.OrderRequestContext;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Default implementation of {@link OrderService}.
 *
 * <p>This implementation enforces deterministic totals validation, idempotent create retries,
 * lifecycle transition rules, and immutable snapshot persistence.</p>
 */
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);

    private static final DateTimeFormatter ORDER_NUMBER_DATE = DateTimeFormatter.ofPattern("yyyyMMdd")
            .withZone(ZoneOffset.UTC);

    private static final Map<OrderStatus, Set<OrderStatus>> STATUS_FLOW = Map.of(
            OrderStatus.CREATED, Set.of(OrderStatus.REVIEWED, OrderStatus.PAYMENT_PENDING, OrderStatus.PAID, OrderStatus.CANCELLED),
            OrderStatus.REVIEWED, Set.of(OrderStatus.PAYMENT_PENDING, OrderStatus.PAID, OrderStatus.CANCELLED),
            OrderStatus.PAYMENT_PENDING, Set.of(OrderStatus.PAID, OrderStatus.CANCELLED),
            OrderStatus.PAID, Set.of(OrderStatus.PROCESSING, OrderStatus.PACKED, OrderStatus.CANCELLED, OrderStatus.REFUNDED),
            OrderStatus.PROCESSING, Set.of(OrderStatus.PACKED, OrderStatus.CANCELLED, OrderStatus.REFUNDED),
            OrderStatus.PACKED, Set.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED, OrderStatus.REFUNDED),
            OrderStatus.SHIPPED, Set.of(OrderStatus.DELIVERED, OrderStatus.REFUNDED),
            OrderStatus.DELIVERED, Set.of(OrderStatus.REFUNDED),
            OrderStatus.CANCELLED, Set.of(OrderStatus.REFUNDED),
            OrderStatus.REFUNDED, Set.of()
    );

    private final OrderRecordRepository orderRecordRepository;
    private final OrderItemRecordRepository orderItemRecordRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final ObjectMapper objectMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public OrderResponse createOrder(OrderRequestContext context, CreateOrderRequest request) {
        String customerRef = resolveCustomerRef(context, request.customerRef());
        String idempotencyKey = normalizeNullable(request.idempotencyKey());
        if (idempotencyKey != null) {
            OrderRecord existingOrder = orderRecordRepository.findByCustomerRefAndIdempotencyKey(customerRef, idempotencyKey)
                    .orElse(null);
            if (existingOrder != null) {
                return toOrderResponse(existingOrder);
            }
        }

        NormalizedTotals totals = normalizeAndValidateTotals(request);
        Instant placedAt = Instant.now();
        OrderStatus initialStatus = Boolean.TRUE.equals(request.paymentConfirmed())
                ? OrderStatus.PAID
                : OrderStatus.PAYMENT_PENDING;

        OrderRecord order = new OrderRecord();
        order.setOrderNumber(generateOrderNumber(placedAt));
        order.setCustomerRef(customerRef);
        order.setStoreId(request.storeId());
        order.setAddressId(request.addressId());
        order.setCurrencyCode(normalizeCurrencyCode(request.currencyCode()));
        order.setSubtotal(totals.subtotal());
        order.setDiscountAmount(totals.discountAmount());
        order.setShippingAmount(totals.shippingAmount());
        order.setTaxAmount(totals.taxAmount());
        order.setTotalAmount(totals.totalAmount());
        order.setPaymentReference(normalizeNullable(request.paymentReference()));
        order.setCouponCode(normalizeNullable(request.couponCode()));
        order.setShippingAddressSnapshotJson(resolveShippingSnapshotJson(request));
        order.setBillingAddressSnapshotJson(toJsonOrNull(request.billingAddress()));
        order.setCheckoutSnapshotJson(toJsonOrNull(request.checkoutContext()));
        order.setStatus(initialStatus);
        order.setRefundStatus(RefundStatus.NONE);
        order.setIdempotencyKey(idempotencyKey);
        order.setPlacedAt(placedAt);
        order.setCreatedBy(context.actorId());
        order.setUpdatedBy(context.actorId());

        try {
            order = orderRecordRepository.save(order);
        } catch (DataIntegrityViolationException ex) {
            OrderRecord existingOrder = resolveExistingOrderForIdempotency(customerRef, idempotencyKey, ex);
            if (existingOrder != null) {
                return toOrderResponse(existingOrder);
            }
            throw ex;
        }

        List<OrderItemRecord> items = new ArrayList<>(request.items().size());
        int lineNumber = 1;
        for (CreateOrderItemRequest itemRequest : request.items()) {
            OrderItemRecord item = new OrderItemRecord();
            item.setOrder(order);
            item.setLineNumber(lineNumber++);
            item.setProductId(itemRequest.productId());
            item.setVariantId(itemRequest.variantId());
            item.setSku(normalizeNullable(itemRequest.sku()));
            item.setProductName(itemRequest.productName().trim());
            item.setVariantName(normalizeNullable(itemRequest.variantName()));
            item.setQuantity(itemRequest.quantity());
            item.setUnitPrice(normalizeMoney(itemRequest.unitPrice()));
            item.setLineTotal(resolveLineTotal(itemRequest));
            item.setItemSnapshotJson(toJsonOrNull(itemRequest.itemSnapshot()));
            item.setCreatedBy(context.actorId());
            item.setUpdatedBy(context.actorId());
            items.add(orderItemRecordRepository.save(item));
        }

        appendStatusHistory(
                order,
                null,
                order.getStatus(),
                order.getRefundStatus(),
                "ORDER_CREATED",
                "Order created from checkout payload",
                context.actorId()
        );

        return toOrderResponse(order, items);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(OrderRequestContext context, UUID orderId) {
        OrderRecord order = orderRecordRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Order not found"));
        assertOrderAccess(context, order);
        return toOrderResponse(order);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> listCustomerOrders(OrderRequestContext context) {
        if (!context.hasSubject()) {
            throw new OrderOperationException(
                    HttpStatus.UNAUTHORIZED,
                    "AUTH_SUBJECT_REQUIRED",
                    "Authenticated customer identity is required"
            );
        }
        return orderRecordRepository.findByCustomerRefOrderByPlacedAtDesc(context.subject()).stream()
                .map(this::toOrderResponse)
                .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> listAdminOrders(
            OrderRequestContext context,
            Pageable pageable,
            String query,
            OrderStatus status,
            RefundStatus refundStatus
    ) {
        assertAdminOrInternal(context);
        Specification<OrderRecord> specification = Specification.where(null);
        if (status != null) {
            specification = specification.and((root, ignoredQuery, cb) -> cb.equal(root.get("status"), status));
        }
        if (refundStatus != null) {
            specification = specification.and((root, ignoredQuery, cb) -> cb.equal(root.get("refundStatus"), refundStatus));
        }
        if (query != null && !query.isBlank()) {
            specification = specification.and(buildQuerySpecification(query.trim()));
        }
        return orderRecordRepository.findAll(specification, pageable).map(this::toOrderResponse);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public OrderResponse updateOrderStatus(OrderRequestContext context, UUID orderId, UpdateOrderStatusRequest request) {
        assertAdminOrInternal(context);
        OrderRecord order = orderRecordRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Order not found"));

        OrderStatus fromStatus = order.getStatus();
        validateStatusTransition(fromStatus, request.status());
        order.setStatus(request.status());
        order.setRefundStatus(request.refundStatus());
        order.setUpdatedBy(context.actorId());
        OrderRecord saved = orderRecordRepository.save(order);

        appendStatusHistory(
                saved,
                fromStatus,
                saved.getStatus(),
                saved.getRefundStatus(),
                normalizeNullable(request.reason()),
                normalizeNullable(request.note()),
                context.actorId()
        );
        return toOrderResponse(saved);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<OrderStatusEventResponse> getOrderTimeline(OrderRequestContext context, UUID orderId) {
        OrderRecord order = orderRecordRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Order not found"));
        assertOrderAccess(context, order);
        return orderStatusHistoryRepository.findByOrderIdOrderByChangedAtAsc(orderId).stream()
                .map(this::toTimelineResponse)
                .toList();
    }

    /**
     * Resolves customer reference for order creation based on actor context.
     *
     * @param context request context
     * @param requestCustomerRef optional customer reference from request payload
     * @return resolved customer reference
     */
    private String resolveCustomerRef(OrderRequestContext context, String requestCustomerRef) {
        if (context.hasSubject()) {
            return context.subject().trim();
        }
        if (context.internalCall()) {
            String normalized = normalizeNullable(requestCustomerRef);
            if (normalized != null) {
                return normalized;
            }
        }
        throw new OrderOperationException(
                HttpStatus.UNAUTHORIZED,
                "AUTH_SUBJECT_REQUIRED",
                "Customer identity is required to create an order"
        );
    }

    /**
     * Enforces admin/internal authorization for privileged operations.
     *
     * @param context request context
     */
    private void assertAdminOrInternal(OrderRequestContext context) {
        if (!context.canManageAllOrders()) {
            throw new OrderOperationException(
                    HttpStatus.FORBIDDEN,
                    "ORDER_ADMIN_FORBIDDEN",
                    "Admin or internal authorization is required"
            );
        }
    }

    /**
     * Enforces order read access for either admin/internal or order owner.
     *
     * @param context request context
     * @param order target order
     */
    private void assertOrderAccess(OrderRequestContext context, OrderRecord order) {
        if (context.canManageAllOrders()) {
            return;
        }
        if (!context.hasSubject()) {
            throw new OrderOperationException(
                    HttpStatus.UNAUTHORIZED,
                    "AUTH_SUBJECT_REQUIRED",
                    "Authenticated customer identity is required"
            );
        }
        if (!context.subject().equals(order.getCustomerRef())) {
            throw new OrderOperationException(HttpStatus.FORBIDDEN, "ORDER_FORBIDDEN", "Order access is forbidden");
        }
    }

    /**
     * Validates requested status transition according to configured state machine.
     *
     * @param current current status
     * @param next requested next status
     */
    private void validateStatusTransition(OrderStatus current, OrderStatus next) {
        if (current == next) {
            return;
        }
        Set<OrderStatus> allowed = STATUS_FLOW.getOrDefault(current, Set.of());
        if (!allowed.contains(next)) {
            throw new OrderOperationException(
                    HttpStatus.BAD_REQUEST,
                    "ORDER_STATUS_INVALID_TRANSITION",
                    "Invalid order status transition from " + current + " to " + next
            );
        }
    }

    /**
     * Builds search specification for admin list query parameter.
     *
     * @param query normalized query text
     * @return composed JPA specification
     */
    private Specification<OrderRecord> buildQuerySpecification(String query) {
        String wildcard = "%" + query.toLowerCase(Locale.ROOT) + "%";
        Specification<OrderRecord> textSpecification = (root, ignoredQuery, cb) -> cb.or(
                cb.like(cb.lower(root.get("orderNumber")), wildcard),
                cb.like(cb.lower(root.get("customerRef")), wildcard),
                cb.and(cb.isNotNull(root.get("couponCode")), cb.like(cb.lower(root.get("couponCode")), wildcard)),
                cb.and(cb.isNotNull(root.get("paymentReference")), cb.like(cb.lower(root.get("paymentReference")), wildcard))
        );

        UUID parsedUuid = tryParseUuid(query);
        if (parsedUuid == null) {
            return textSpecification;
        }
        return textSpecification.or((root, ignoredQuery, cb) -> cb.or(
                cb.equal(root.get("id"), parsedUuid),
                cb.equal(root.get("storeId"), parsedUuid),
                cb.equal(root.get("addressId"), parsedUuid)
        ));
    }

    /**
     * Parses a UUID from a string when possible.
     *
     * @param value source value
     * @return parsed UUID or {@code null}
     */
    private UUID tryParseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /**
     * Normalizes request totals and validates arithmetic determinism rules.
     *
     * @param request create order payload
     * @return normalized totals model
     */
    private NormalizedTotals normalizeAndValidateTotals(CreateOrderRequest request) {
        BigDecimal expectedSubtotal = request.items().stream()
                .map(this::resolveLineTotal)
                .reduce(ZERO, BigDecimal::add);

        BigDecimal subtotal = normalizeMoney(request.subtotal());
        BigDecimal discountAmount = normalizeMoney(request.discountAmount());
        BigDecimal shippingAmount = normalizeMoney(request.shippingAmount());
        BigDecimal taxAmount = normalizeMoney(request.taxAmount());
        BigDecimal totalAmount = normalizeMoney(request.totalAmount());

        if (expectedSubtotal.compareTo(subtotal) != 0) {
            throw new OrderOperationException(
                    HttpStatus.BAD_REQUEST,
                    "ORDER_SUBTOTAL_MISMATCH",
                    "Subtotal does not match order line totals"
            );
        }

        BigDecimal expectedTotal = subtotal
                .subtract(discountAmount)
                .add(shippingAmount)
                .add(taxAmount)
                .setScale(4, RoundingMode.HALF_UP);
        if (expectedTotal.compareTo(totalAmount) != 0) {
            throw new OrderOperationException(
                    HttpStatus.BAD_REQUEST,
                    "ORDER_TOTAL_MISMATCH",
                    "Total amount does not match subtotal/discount/shipping/tax composition"
            );
        }

        return new NormalizedTotals(subtotal, discountAmount, shippingAmount, taxAmount, totalAmount);
    }

    /**
     * Resolves line total from request payload, computing it when omitted.
     *
     * @param itemRequest line payload
     * @return normalized line total
     */
    private BigDecimal resolveLineTotal(CreateOrderItemRequest itemRequest) {
        BigDecimal unitPrice = normalizeMoney(itemRequest.unitPrice());
        BigDecimal calculated = unitPrice
                .multiply(BigDecimal.valueOf(itemRequest.quantity()))
                .setScale(4, RoundingMode.HALF_UP);
        if (itemRequest.lineTotal() == null) {
            return calculated;
        }
        BigDecimal provided = normalizeMoney(itemRequest.lineTotal());
        if (provided.compareTo(calculated) != 0) {
            throw new OrderOperationException(
                    HttpStatus.BAD_REQUEST,
                    "ORDER_LINE_TOTAL_MISMATCH",
                    "Line total does not match quantity * unitPrice for product " + itemRequest.productId()
            );
        }
        return provided;
    }

    /**
     * Resolves shipping snapshot JSON from structured or plain-text payload fields.
     *
     * @param request create order payload
     * @return serialized shipping snapshot JSON, or null
     */
    private String resolveShippingSnapshotJson(CreateOrderRequest request) {
        if (request.shippingAddress() != null) {
            return toJsonOrNull(request.shippingAddress());
        }
        String plain = normalizeNullable(request.shippingAddressSnapshot());
        if (plain == null) {
            return null;
        }
        Map<String, Object> fallback = new LinkedHashMap<>();
        fallback.put("text", plain);
        return toJsonOrNull(fallback);
    }

    /**
     * Serializes arbitrary object to JSON text.
     *
     * @param value source object
     * @return serialized JSON or null
     */
    private String toJsonOrNull(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new OrderOperationException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "ORDER_SNAPSHOT_SERIALIZATION_FAILED",
                    "Failed to serialize order snapshot payload"
            );
        }
    }

    /**
     * Deserializes snapshot JSON to generic object for response payloads.
     *
     * @param json source JSON text
     * @return deserialized object or source string fallback
     */
    private Object fromJsonOrRaw(String json) {
        String normalized = normalizeNullable(json);
        if (normalized == null) {
            return null;
        }
        try {
            return objectMapper.readValue(normalized, Object.class);
        } catch (JsonProcessingException ex) {
            return normalized;
        }
    }

    /**
     * Normalizes currency code to upper-case.
     *
     * @param value currency code
     * @return normalized currency code
     */
    private String normalizeCurrencyCode(String value) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            throw new OrderOperationException(HttpStatus.BAD_REQUEST, "ORDER_CURRENCY_REQUIRED", "currencyCode is required");
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

    /**
     * Normalizes monetary values to scale 4.
     *
     * @param value source value
     * @return normalized value
     */
    private BigDecimal normalizeMoney(BigDecimal value) {
        if (value == null) {
            throw new OrderOperationException(HttpStatus.BAD_REQUEST, "ORDER_AMOUNT_REQUIRED", "Monetary fields are required");
        }
        BigDecimal normalized = value.setScale(4, RoundingMode.HALF_UP);
        if (normalized.compareTo(BigDecimal.ZERO) < 0) {
            throw new OrderOperationException(
                    HttpStatus.BAD_REQUEST,
                    "ORDER_AMOUNT_NEGATIVE",
                    "Monetary values must be non-negative"
            );
        }
        return normalized;
    }

    /**
     * Generates a human-readable business order number.
     *
     * @param placedAt placement timestamp
     * @return generated order number
     */
    private String generateOrderNumber(Instant placedAt) {
        return "ORD-" + ORDER_NUMBER_DATE.format(placedAt) + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    /**
     * Resolves existing idempotent order when a concurrent unique constraint race occurs.
     *
     * @param customerRef customer reference
     * @param idempotencyKey idempotency key
     * @param rootCause root exception
     * @return existing order or null
     */
    private OrderRecord resolveExistingOrderForIdempotency(
            String customerRef,
            String idempotencyKey,
            DataIntegrityViolationException rootCause
    ) {
        if (idempotencyKey == null) {
            return null;
        }
        try {
            return orderRecordRepository.findByCustomerRefAndIdempotencyKey(customerRef, idempotencyKey).orElse(null);
        } catch (RuntimeException ignored) {
            throw rootCause;
        }
    }

    /**
     * Appends one status transition history event.
     *
     * @param order target order
     * @param fromStatus previous status
     * @param toStatus next status
     * @param refundStatus refund status
     * @param reason transition reason
     * @param note transition note
     * @param actor actor identifier
     */
    private void appendStatusHistory(
            OrderRecord order,
            OrderStatus fromStatus,
            OrderStatus toStatus,
            RefundStatus refundStatus,
            String reason,
            String note,
            String actor
    ) {
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setFromStatus(fromStatus);
        history.setToStatus(toStatus);
        history.setRefundStatus(refundStatus == null ? RefundStatus.NONE : refundStatus);
        history.setReason(normalizeNullable(reason));
        history.setNote(normalizeNullable(note));
        history.setChangedBy(actor);
        history.setChangedAt(Instant.now());
        history.setCreatedBy(actor);
        history.setUpdatedBy(actor);
        orderStatusHistoryRepository.save(history);
    }

    /**
     * Maps order aggregate to response DTO by loading line items.
     *
     * @param order order aggregate
     * @return mapped response DTO
     */
    private OrderResponse toOrderResponse(OrderRecord order) {
        List<OrderItemRecord> items = orderItemRecordRepository.findByOrderIdOrderByLineNumberAsc(order.getId());
        return toOrderResponse(order, items);
    }

    /**
     * Maps order aggregate and line item list to response DTO.
     *
     * @param order order aggregate
     * @param items order line items
     * @return mapped response DTO
     */
    private OrderResponse toOrderResponse(OrderRecord order, List<OrderItemRecord> items) {
        List<OrderItemResponse> lineResponses = items.stream()
                .map(item -> new OrderItemResponse(
                        item.getId(),
                        item.getLineNumber(),
                        item.getProductId(),
                        item.getVariantId(),
                        item.getSku(),
                        item.getProductName(),
                        item.getVariantName(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getLineTotal()
                ))
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getCustomerRef(),
                order.getCustomerRef(),
                order.getStoreId(),
                order.getAddressId(),
                order.getSubtotal(),
                order.getDiscountAmount(),
                order.getShippingAmount(),
                order.getTaxAmount(),
                order.getTotalAmount(),
                order.getCurrencyCode(),
                order.getStatus(),
                order.getRefundStatus(),
                order.getPaymentReference(),
                order.getCouponCode(),
                fromJsonOrRaw(order.getShippingAddressSnapshotJson()),
                fromJsonOrRaw(order.getBillingAddressSnapshotJson()),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                order.getPlacedAt(),
                lineResponses
        );
    }

    /**
     * Maps status history entry to timeline response DTO.
     *
     * @param history history entry
     * @return timeline response DTO
     */
    private OrderStatusEventResponse toTimelineResponse(OrderStatusHistory history) {
        return new OrderStatusEventResponse(
                history.getId(),
                history.getOrderId(),
                history.getToStatus(),
                history.getFromStatus(),
                history.getToStatus(),
                history.getRefundStatus(),
                history.getChangedBy(),
                history.getReason(),
                history.getNote(),
                history.getChangedAt()
        );
    }

    /**
     * Trims input and returns null for blank strings.
     *
     * @param value source text
     * @return normalized text or null
     */
    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * Normalized totals record.
     *
     * @param subtotal subtotal
     * @param discountAmount discount
     * @param shippingAmount shipping
     * @param taxAmount tax
     * @param totalAmount total
     */
    private record NormalizedTotals(
            BigDecimal subtotal,
            BigDecimal discountAmount,
            BigDecimal shippingAmount,
            BigDecimal taxAmount,
            BigDecimal totalAmount
    ) {
    }
}

