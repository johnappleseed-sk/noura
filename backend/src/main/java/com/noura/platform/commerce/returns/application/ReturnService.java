package com.noura.platform.commerce.returns.application;

import com.noura.platform.commerce.customers.domain.CustomerAccount;
import com.noura.platform.commerce.customers.infrastructure.CustomerAccountRepo;
import com.noura.platform.commerce.notifications.application.NotificationService;
import com.noura.platform.commerce.orders.domain.Order;
import com.noura.platform.commerce.orders.domain.OrderItem;
import com.noura.platform.commerce.orders.domain.OrderStatus;
import com.noura.platform.commerce.orders.infrastructure.OrderItemRepo;
import com.noura.platform.commerce.orders.infrastructure.OrderRepo;
import com.noura.platform.commerce.returns.domain.ReturnItem;
import com.noura.platform.commerce.returns.domain.ReturnReason;
import com.noura.platform.commerce.returns.domain.ReturnRequest;
import com.noura.platform.commerce.returns.domain.ReturnStatus;
import com.noura.platform.commerce.returns.infrastructure.ReturnItemRepo;
import com.noura.platform.commerce.returns.infrastructure.ReturnRequestRepo;
import com.noura.platform.dto.returns.CreateReturnItemRequest;
import com.noura.platform.dto.returns.CreateReturnRequest;
import com.noura.platform.dto.returns.ReceiveItemsRequest;
import com.noura.platform.dto.returns.ReturnItemDto;
import com.noura.platform.dto.returns.ReturnItemQuantity;
import com.noura.platform.dto.returns.ReturnRequestDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing return/RMA requests.
 */
@Service
@Transactional
public class ReturnService {
    private static final Logger log = LoggerFactory.getLogger(ReturnService.class);
    private static final DateTimeFormatter RETURN_NUMBER_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int MAX_REASON_DETAILS_LENGTH = 1000;
    private static final int MAX_NOTES_LENGTH = 1000;

    private final ReturnRequestRepo returnRequestRepo;
    private final ReturnItemRepo returnItemRepo;
    private final OrderRepo orderRepo;
    private final OrderItemRepo orderItemRepo;
    private final CustomerAccountRepo customerAccountRepo;
    private final NotificationService notificationService;

    public ReturnService(ReturnRequestRepo returnRequestRepo,
                        ReturnItemRepo returnItemRepo,
                        OrderRepo orderRepo,
                        OrderItemRepo orderItemRepo,
                        CustomerAccountRepo customerAccountRepo,
                        NotificationService notificationService) {
        this.returnRequestRepo = returnRequestRepo;
        this.returnItemRepo = returnItemRepo;
        this.orderRepo = orderRepo;
        this.orderItemRepo = orderItemRepo;
        this.customerAccountRepo = customerAccountRepo;
        this.notificationService = notificationService;
    }

    // ===============================
    // Customer-facing Operations
    // ===============================

    /**
     * Create a new return request for a customer.
     */
    public ReturnRequestDto createReturnRequest(Long customerId, CreateReturnRequest request) {
        if (request == null || request.orderId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order ID is required.");
        }
        if (request.items() == null || request.items().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one item is required for return.");
        }

        Order order = orderRepo.findById(request.orderId())
                .filter(o -> o.getCustomerAccount() != null && customerId.equals(o.getCustomerAccount().getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found."));

        validateOrderCanBeReturned(order);

        CustomerAccount customer = order.getCustomerAccount();
        ReturnRequest returnRequest = new ReturnRequest();
        returnRequest.setReturnNumber(generateReturnNumber());
        returnRequest.setOrder(order);
        returnRequest.setCustomerAccount(customer);
        returnRequest.setStatus(ReturnStatus.PENDING_REVIEW);
        returnRequest.setReason(parseReason(request.reason()));
        returnRequest.setReasonDetails(truncate(request.reasonDetails(), MAX_REASON_DETAILS_LENGTH));
        returnRequest.setCustomerNotes(truncate(request.customerNotes(), MAX_NOTES_LENGTH));
        returnRequest.setCurrencyCode(order.getCurrencyCode());

        BigDecimal totalRefundAmount = BigDecimal.ZERO;

        for (CreateReturnItemRequest itemRequest : request.items()) {
            OrderItem orderItem = orderItemRepo.findById(itemRequest.orderItemId())
                    .filter(oi -> oi.getOrder().getId().equals(order.getId()))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Order item not found: " + itemRequest.orderItemId()));

            int quantity = itemRequest.quantity() != null ? itemRequest.quantity() : 1;
            if (quantity <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be positive.");
            }
            if (quantity > orderItem.getQuantity().intValue()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Return quantity exceeds ordered quantity for item: " + orderItem.getProductName());
            }

            ReturnItem returnItem = new ReturnItem();
            returnItem.setOrderItem(orderItem);
            returnItem.setQuantityRequested(quantity);
            returnItem.setProductName(orderItem.getProductName());
            returnItem.setSku(orderItem.getSku());
            returnItem.setUnitRefundAmount(orderItem.getUnitPrice());
            BigDecimal lineRefund = orderItem.getUnitPrice().multiply(BigDecimal.valueOf(quantity))
                    .setScale(4, RoundingMode.HALF_UP);
            returnItem.setLineRefundAmount(lineRefund);
            totalRefundAmount = totalRefundAmount.add(lineRefund);

            returnRequest.addItem(returnItem);
        }

        returnRequest.setRefundAmount(totalRefundAmount);
        ReturnRequest saved = returnRequestRepo.save(returnRequest);

        // Send notification
        notificationService.sendReturnRequestReceived(
                saved.getId(),
                customer.getEmail(),
                saved.getReturnNumber()
        );

        log.info("Created return request {} for order {} by customer {}",
                saved.getReturnNumber(), order.getOrderNumber(), customerId);

        return toDto(saved);
    }

    /**
     * Get return request details for a customer.
     */
    @Transactional(readOnly = true)
    public ReturnRequestDto getReturnRequest(Long customerId, Long returnId) {
        ReturnRequest returnRequest = returnRequestRepo.findByIdAndCustomerAccount_Id(returnId, customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Return request not found."));
        return toDto(returnRequest);
    }

    /**
     * List all return requests for a customer.
     */
    @Transactional(readOnly = true)
    public List<ReturnRequestDto> listReturnRequests(Long customerId) {
        return returnRequestRepo.findByCustomerAccount_IdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Cancel a pending return request.
     */
    public ReturnRequestDto cancelReturnRequest(Long customerId, Long returnId) {
        ReturnRequest returnRequest = returnRequestRepo.findByIdAndCustomerAccount_Id(returnId, customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Return request not found."));

        if (returnRequest.getStatus() != ReturnStatus.PENDING_REVIEW) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only pending returns can be cancelled. Current status: " + returnRequest.getStatus());
        }

        returnRequest.setStatus(ReturnStatus.CANCELLED);
        log.info("Customer {} cancelled return request {}", customerId, returnRequest.getReturnNumber());

        return toDto(returnRequest);
    }

    // ===============================
    // Staff Operations
    // ===============================

    /**
     * Approve a return request.
     */
    public ReturnRequestDto approveReturn(Long returnId, String staffUsername, String instructions) {
        ReturnRequest returnRequest = returnRequestRepo.findById(returnId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Return request not found."));

        if (returnRequest.getStatus() != ReturnStatus.PENDING_REVIEW) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only pending returns can be approved. Current status: " + returnRequest.getStatus());
        }

        returnRequest.setStatus(ReturnStatus.APPROVED);
        returnRequest.setReviewedBy(staffUsername);
        returnRequest.setReviewedAt(LocalDateTime.now());

        // Set approved quantities to requested quantities by default
        for (ReturnItem item : returnRequest.getItems()) {
            item.setQuantityApproved(item.getQuantityRequested());
        }

        // Send notification
        CustomerAccount customer = returnRequest.getCustomerAccount();
        if (customer != null && customer.getEmail() != null) {
            notificationService.sendReturnApproved(
                    returnRequest.getId(),
                    customer.getEmail(),
                    returnRequest.getReturnNumber(),
                    instructions
            );
        }

        log.info("Staff {} approved return request {}", staffUsername, returnRequest.getReturnNumber());

        return toDto(returnRequest);
    }

    /**
     * Reject a return request.
     */
    public ReturnRequestDto rejectReturn(Long returnId, String staffUsername, String reason) {
        ReturnRequest returnRequest = returnRequestRepo.findById(returnId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Return request not found."));

        if (returnRequest.getStatus() != ReturnStatus.PENDING_REVIEW) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only pending returns can be rejected. Current status: " + returnRequest.getStatus());
        }

        returnRequest.setStatus(ReturnStatus.REJECTED);
        returnRequest.setReviewedBy(staffUsername);
        returnRequest.setReviewedAt(LocalDateTime.now());
        returnRequest.setStaffNotes(truncate(reason, MAX_NOTES_LENGTH));

        // Send notification
        CustomerAccount customer = returnRequest.getCustomerAccount();
        if (customer != null && customer.getEmail() != null) {
            notificationService.sendReturnRejected(
                    returnRequest.getId(),
                    customer.getEmail(),
                    returnRequest.getReturnNumber(),
                    reason
            );
        }

        log.info("Staff {} rejected return request {}: {}", staffUsername, returnRequest.getReturnNumber(), reason);

        return toDto(returnRequest);
    }

    /**
     * Mark items as received at warehouse.
     */
    public ReturnRequestDto markItemsReceived(Long returnId, String staffUsername, ReceiveItemsRequest request) {
        ReturnRequest returnRequest = returnRequestRepo.findById(returnId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Return request not found."));

        if (returnRequest.getStatus() != ReturnStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only approved returns can receive items. Current status: " + returnRequest.getStatus());
        }

        // Update received quantities if provided
        if (request != null && request.itemQuantities() != null) {
            for (ReturnItemQuantity iq : request.itemQuantities()) {
                returnRequest.getItems().stream()
                        .filter(item -> item.getId().equals(iq.returnItemId()))
                        .findFirst()
                        .ifPresent(item -> {
                            item.setQuantityReceived(iq.quantityReceived());
                            item.setConditionNotes(truncate(iq.conditionNotes(), 500));
                        });
            }
        } else {
            // Default: received = approved
            for (ReturnItem item : returnRequest.getItems()) {
                if (item.getQuantityReceived() == null) {
                    item.setQuantityReceived(item.getQuantityApproved());
                }
            }
        }

        returnRequest.setStatus(ReturnStatus.ITEMS_RECEIVED);
        returnRequest.setItemsReceivedAt(LocalDateTime.now());

        log.info("Staff {} marked items received for return {}", staffUsername, returnRequest.getReturnNumber());

        return toDto(returnRequest);
    }

    /**
     * Process refund for a return.
     */
    public ReturnRequestDto processRefund(Long returnId, String staffUsername) {
        ReturnRequest returnRequest = returnRequestRepo.findById(returnId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Return request not found."));

        if (returnRequest.getStatus() != ReturnStatus.ITEMS_RECEIVED
                && returnRequest.getStatus() != ReturnStatus.PROCESSING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Return must have items received before processing refund. Current status: " + returnRequest.getStatus());
        }

        // Calculate actual refund based on received quantities
        BigDecimal actualRefund = BigDecimal.ZERO;
        for (ReturnItem item : returnRequest.getItems()) {
            int qty = item.getQuantityReceived() != null ? item.getQuantityReceived() : 0;
            if (qty > 0 && item.getUnitRefundAmount() != null) {
                BigDecimal lineRefund = item.getUnitRefundAmount()
                        .multiply(BigDecimal.valueOf(qty))
                        .setScale(4, RoundingMode.HALF_UP);
                item.setLineRefundAmount(lineRefund);
                actualRefund = actualRefund.add(lineRefund);
            }
        }

        returnRequest.setRefundAmount(actualRefund);
        returnRequest.setStatus(ReturnStatus.REFUNDED);
        returnRequest.setRefundedAt(LocalDateTime.now());

        // TODO: Integrate with payment service to actually process refund

        log.info("Staff {} processed refund {} {} for return {}",
                staffUsername, returnRequest.getCurrencyCode(), actualRefund, returnRequest.getReturnNumber());

        return toDto(returnRequest);
    }

    /**
     * Complete a return (final state).
     */
    public ReturnRequestDto completeReturn(Long returnId, String staffUsername) {
        ReturnRequest returnRequest = returnRequestRepo.findById(returnId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Return request not found."));

        if (returnRequest.getStatus() != ReturnStatus.REFUNDED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Return must be refunded before completion. Current status: " + returnRequest.getStatus());
        }

        returnRequest.setStatus(ReturnStatus.COMPLETED);

        // TODO: Restock items if marked for restock

        log.info("Staff {} completed return {}", staffUsername, returnRequest.getReturnNumber());

        return toDto(returnRequest);
    }

    /**
     * List all returns for staff review.
     */
    @Transactional(readOnly = true)
    public Page<ReturnRequestDto> listAllReturns(Pageable pageable) {
        return returnRequestRepo.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::toDto);
    }

    /**
     * List returns by status for staff.
     */
    @Transactional(readOnly = true)
    public Page<ReturnRequestDto> listReturnsByStatus(ReturnStatus status, Pageable pageable) {
        return returnRequestRepo.findByStatus(status, pageable)
                .map(this::toDto);
    }

    /**
     * Get return details for staff.
     */
    @Transactional(readOnly = true)
    public ReturnRequestDto getReturnForStaff(Long returnId) {
        ReturnRequest returnRequest = returnRequestRepo.findById(returnId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Return request not found."));
        return toDto(returnRequest);
    }

    // ===============================
    // Helper Methods
    // ===============================

    private void validateOrderCanBeReturned(Order order) {
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot return a cancelled order.");
        }
        if (order.getStatus() == OrderStatus.DRAFT || order.getStatus() == OrderStatus.PENDING_PAYMENT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot return an unpaid order.");
        }
        // Add more validation rules (e.g., return window, etc.)
    }

    private String generateReturnNumber() {
        String datePart = LocalDateTime.now().format(RETURN_NUMBER_DATE_FORMAT);
        String randomPart = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String returnNumber = "RMA-" + datePart + "-" + randomPart;

        // Ensure uniqueness
        int attempts = 0;
        while (returnRequestRepo.existsByReturnNumber(returnNumber) && attempts < 10) {
            randomPart = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            returnNumber = "RMA-" + datePart + "-" + randomPart;
            attempts++;
        }

        return returnNumber;
    }

    private ReturnReason parseReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return ReturnReason.OTHER;
        }
        try {
            return ReturnReason.valueOf(reason.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ReturnReason.OTHER;
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    private ReturnRequestDto toDto(ReturnRequest r) {
        List<ReturnItemDto> itemDtos = r.getItems().stream()
                .map(this::toItemDto)
                .toList();

        return new ReturnRequestDto(
                r.getId(),
                r.getReturnNumber(),
                r.getOrder() != null ? r.getOrder().getId() : null,
                r.getOrder() != null ? r.getOrder().getOrderNumber() : null,
                r.getCustomerAccount() != null ? r.getCustomerAccount().getId() : null,
                r.getStatus().name(),
                r.getReason() != null ? r.getReason().name() : null,
                r.getReasonDetails(),
                r.getCustomerNotes(),
                r.getStaffNotes(),
                r.getRefundAmount(),
                r.getCurrencyCode(),
                r.getReturnTrackingNumber(),
                r.getReturnCarrier(),
                r.getReviewedBy(),
                r.getReviewedAt(),
                r.getItemsReceivedAt(),
                r.getRefundedAt(),
                r.getCreatedAt(),
                r.getUpdatedAt(),
                itemDtos
        );
    }

    private ReturnItemDto toItemDto(ReturnItem i) {
        return new ReturnItemDto(
                i.getId(),
                i.getOrderItem() != null ? i.getOrderItem().getId() : null,
                i.getProductName(),
                i.getSku(),
                i.getQuantityRequested(),
                i.getQuantityReceived(),
                i.getQuantityApproved(),
                i.getUnitRefundAmount(),
                i.getLineRefundAmount(),
                i.getConditionNotes(),
                i.isRestock()
        );
    }

}
