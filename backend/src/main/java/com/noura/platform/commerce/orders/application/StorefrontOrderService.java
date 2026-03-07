package com.noura.platform.commerce.orders.application;

import com.noura.platform.commerce.entity.Product;
import com.noura.platform.commerce.cart.application.StorefrontCartService;
import com.noura.platform.commerce.cart.domain.Cart;
import com.noura.platform.commerce.cart.domain.CartItem;
import com.noura.platform.commerce.cart.domain.CartStatus;
import com.noura.platform.commerce.customers.domain.CustomerAccount;
import com.noura.platform.commerce.customers.domain.CustomerAddress;
import com.noura.platform.commerce.customers.infrastructure.CustomerAccountRepo;
import com.noura.platform.commerce.customers.infrastructure.CustomerAddressRepo;
import com.noura.platform.commerce.fulfillment.domain.ShipmentStatus;
import com.noura.platform.commerce.fulfillment.infrastructure.ShipmentRepo;
import com.noura.platform.commerce.orders.domain.Order;
import com.noura.platform.commerce.orders.domain.OrderItem;
import com.noura.platform.commerce.orders.domain.OrderStatus;
import com.noura.platform.commerce.orders.infrastructure.OrderRepo;
import com.noura.platform.commerce.payments.application.StorefrontPaymentService;
import com.noura.platform.commerce.repository.ProductRepo;
import com.noura.platform.commerce.service.StockMovementService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Transactional
public class StorefrontOrderService {
    private static final BigDecimal ZERO_MONEY = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
    private static final DateTimeFormatter ORDER_PREFIX_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final StorefrontCartService cartService;
    private final OrderRepo orderRepo;
    private final CustomerAccountRepo customerAccountRepo;
    private final CustomerAddressRepo customerAddressRepo;
    private final ProductRepo productRepo;
    private final ShipmentRepo shipmentRepo;
    private final StorefrontPaymentService paymentService;
    private final StockMovementService stockMovementService;

    @Value("${app.currency.base:USD}")
    private String defaultCurrency;

    public StorefrontOrderService(StorefrontCartService cartService,
                                  OrderRepo orderRepo,
                                  CustomerAccountRepo customerAccountRepo,
                                  CustomerAddressRepo customerAddressRepo,
                                  ProductRepo productRepo,
                                  ShipmentRepo shipmentRepo,
                                  StorefrontPaymentService paymentService,
                                  StockMovementService stockMovementService) {
        this.cartService = cartService;
        this.orderRepo = orderRepo;
        this.customerAccountRepo = customerAccountRepo;
        this.customerAddressRepo = customerAddressRepo;
        this.productRepo = productRepo;
        this.shipmentRepo = shipmentRepo;
        this.paymentService = paymentService;
        this.stockMovementService = stockMovementService;
    }

    public List<OrderSummaryDto> listOrders(Long customerId) {
        CustomerAccount customer = customerAccountRepo.findById(customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Customer not found."));
        List<Order> orders = orderRepo.findByCustomerAccount_IdOrderByPlacedAtDesc(customer.getId());
        return orders.stream()
                .map(this::toSummaryDto)
                .toList();
    }

    public StorefrontOrderResult checkout(Long customerId, CheckoutRequest request) {
        Cart cart = cartService.resolveActiveCartForCheckout(customerId);
        CustomerAccount customer = customerAccountRepo.findById(customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Customer not found."));
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart is empty.");
        }

        CustomerAddress shippingAddress = resolveShippingAddress(customer, request == null ? null : request.shippingAddressId());
        String currency = normalizeCurrency(request == null ? null : request.currency());
        String orderNumber = generateOrderNumber(customerId);

        Order order = new Order();
        order.setOrderNumber(orderNumber);
        order.setCustomerAccount(customer);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setCurrencyCode(currency);
        order.setCustomerEmail(customer.getEmail());
        order.setCustomerPhone(customer.getPhone());
        applyShippingAddress(order, shippingAddress);
        order.setDiscountTotal(ZERO_MONEY);
        order.setTaxTotal(ZERO_MONEY);
        order.setShippingTotal(ZERO_MONEY);

        BigDecimal subtotal = ZERO_MONEY;
        for (CartItem cartItem : List.copyOf(cart.getItems())) {
            Long productId = cartItem.getProductId();
            if (productId == null || productId <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart contains invalid product.");
            }
            Product product = productRepo.findById(productId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found."));
            if (!Boolean.TRUE.equals(product.getActive())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product " + product.getName() + " is unavailable.");
            }
            int qty = cartItem.getQuantity();

            OrderItem orderItem = new OrderItem();
            orderItem.setProductId(productId);
            orderItem.setVariantId(null);
            orderItem.setSku(product.getSku());
            orderItem.setProductName(product.getName());
            orderItem.setUnitLabel(cartItem.getUnitLabel());
            orderItem.setQuantity(BigDecimal.valueOf(qty));
            orderItem.setUnitPrice(safeMoney(cartItem.getUnitPrice()));
            orderItem.setLineTotal(safeMoney(cartItem.getUnitPrice())
                    .multiply(BigDecimal.valueOf(qty))
                    .setScale(4, RoundingMode.HALF_UP));
            orderItem.setOrder(order);
            order.getItems().add(orderItem);

            subtotal = subtotal.add(orderItem.getLineTotal());
            stockMovementService.recordSale(
                    productId,
                    qty,
                    safeMoney(product.getPrice()),
                    currency,
                    "WEB_ORDER",
                    orderNumber,
                    null,
                    "Storefront checkout."
            );
        }

        order.setSubtotal(subtotal);
        order.setGrandTotal(subtotal);
        order.setPlacedAt(LocalDateTime.now());
        Order saved = orderRepo.save(order);
        ensureInitialShipment(saved);
        StorefrontPaymentService.PaymentTransactionResult payment = paymentService.createInitialPayment(
                customerId,
                saved.getId(),
                new StorefrontPaymentService.CreatePaymentRequest(
                        request == null ? null : request.paymentMethod(),
                        request == null ? null : request.paymentProvider(),
                        request == null ? null : request.paymentProviderReference()
                )
        );

        // Convert cart only after order persists to avoid losing intent during rollback.
        cart.setStatus(CartStatus.CONVERTED);
        cartService.removeActiveItemsAfterCheckout(cart);

        return toResult(saved, payment);
    }

    public StorefrontOrderResult getOrder(Long customerId, Long orderId) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found."));
        if (order.getCustomerAccount() == null || !customerId.equals(order.getCustomerAccount().getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found.");
        }
        if (order.getItems() == null) {
            order.setItems(List.of());
        }
        return toResult(order, paymentService.getLatestForOrder(order));
    }

    public StorefrontOrderResult cancel(Long customerId, Long orderId) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found."));
        if (order.getCustomerAccount() == null || !customerId.equals(order.getCustomerAccount().getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found.");
        }
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.REFUNDED) {
            return toResult(order, paymentService.getLatestForOrder(order));
        }
        if (!isCancellableStatus(order.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order cannot be cancelled in current status.");
        }

        List<OrderItem> items = order.getItems() == null ? List.of() : order.getItems();
        for (OrderItem orderItem : items) {
            if (orderItem == null || orderItem.getProductId() == null) {
                continue;
            }
            if (orderItem.getQuantity() == null || orderItem.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            int qty = orderItem.getQuantity().intValue();
            if (qty <= 0) {
                continue;
            }
            stockMovementService.recordReturn(
                    orderItem.getProductId(),
                    qty,
                    safeMoney(orderItem.getUnitPrice()),
                    order.getCurrencyCode(),
                    "WEB_ORDER",
                    order.getOrderNumber(),
                    null,
                    "Storefront order cancellation."
            );
        }

        paymentService.refundAll(customerId, order.getId(),
                "Order cancelled by customer.");
        order.setStatus(OrderStatus.CANCELLED);
        markShipmentCancelled(order);
        return toResult(orderRepo.save(order), paymentService.getLatestForOrder(order));
    }

    private boolean isCancellableStatus(OrderStatus status) {
        return status == null
                || status == OrderStatus.DRAFT
                || status == OrderStatus.PENDING_PAYMENT
                || status == OrderStatus.PAID
                || status == OrderStatus.PROCESSING;
    }

    private StorefrontOrderResult toResult(Order order, StorefrontPaymentService.PaymentTransactionResult payment) {
        List<OrderItem> items = order.getItems() == null ? List.of() : order.getItems();
        return new StorefrontOrderResult(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus() == null ? null : order.getStatus().name(),
                order.getCurrencyCode(),
                order.getSubtotal(),
                order.getDiscountTotal(),
                order.getTaxTotal(),
                order.getShippingTotal(),
                order.getGrandTotal(),
                order.getPlacedAt() == null ? null : order.getPlacedAt(),
                toShippingAddressDto(order),
                payment,
                items.stream()
                        .map(item -> new StorefrontOrderItemDto(
                                item.getId(),
                                item.getProductId(),
                                item.getSku(),
                                item.getProductName(),
                                item.getQuantity(),
                                item.getUnitPrice(),
                                item.getLineTotal()
                        ))
                        .toList()
        );
    }

    private void ensureInitialShipment(Order order) {
        if (order == null || order.getId() == null) {
            return;
        }
        shipmentRepo.findTopByOrder_IdOrderByCreatedAtDesc(order.getId()).orElseGet(() -> {
            var shipment = new com.noura.platform.commerce.fulfillment.domain.Shipment();
            shipment.setOrder(order);
            shipment.setStatus(ShipmentStatus.PENDING);
            return shipmentRepo.save(shipment);
        });
    }

    private void markShipmentCancelled(Order order) {
        if (order == null || order.getId() == null) {
            return;
        }
        shipmentRepo.findTopByOrder_IdOrderByCreatedAtDesc(order.getId()).ifPresent(shipment -> {
            if (shipment.getStatus() != ShipmentStatus.CANCELLED) {
                shipment.setStatus(ShipmentStatus.CANCELLED);
                shipmentRepo.save(shipment);
            }
        });
    }

    private String generateOrderNumber(Long customerId) {
        long dayMarker = Long.parseLong(LocalDate.now().format(ORDER_PREFIX_FORMAT));
        int rand = ThreadLocalRandom.current().nextInt(1000, 9999);
        return String.format("ORD-%d-%d-%04d", dayMarker, customerId, rand);
    }

    private String normalizeCurrency(String currencyCode) {
        String normalized = currencyCode == null ? "" : currencyCode.trim().toUpperCase();
        if (normalized.isBlank()) {
            normalized = defaultCurrency == null ? "USD" : defaultCurrency.trim().toUpperCase();
        }
        return normalized.isBlank() ? "USD" : normalized;
    }

    private CustomerAddress resolveShippingAddress(CustomerAccount customer, Long shippingAddressId) {
        if (shippingAddressId != null) {
            if (shippingAddressId <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "shippingAddressId must be a positive number.");
            }
            return customerAddressRepo.findByIdAndCustomerAccount_Id(shippingAddressId, customer.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shipping address not found."));
        }
        return customerAddressRepo.findTopByCustomerAccount_IdAndDefaultShippingTrueOrderByIdAsc(customer.getId())
                .orElseGet(() -> customerAddressRepo.findFirstByCustomerAccount_IdOrderByIdAsc(customer.getId()).orElse(null));
    }

    private void applyShippingAddress(Order order, CustomerAddress shippingAddress) {
        if (shippingAddress == null) {
            return;
        }
        order.setShippingRecipientName(shippingAddress.getRecipientName());
        order.setShippingPhone(shippingAddress.getPhone());
        order.setShippingLine1(shippingAddress.getLine1());
        order.setShippingLine2(shippingAddress.getLine2());
        order.setShippingDistrict(shippingAddress.getDistrict());
        order.setShippingCity(shippingAddress.getCity());
        order.setShippingStateProvince(shippingAddress.getStateProvince());
        order.setShippingPostalCode(shippingAddress.getPostalCode());
        order.setShippingCountryCode(shippingAddress.getCountryCode());
    }

    private StorefrontOrderShippingAddressDto toShippingAddressDto(Order order) {
        if (order.getShippingLine1() == null
                && order.getShippingRecipientName() == null
                && order.getShippingCity() == null
                && order.getShippingCountryCode() == null) {
            return null;
        }
        return new StorefrontOrderShippingAddressDto(
                order.getShippingRecipientName(),
                order.getShippingPhone(),
                order.getShippingLine1(),
                order.getShippingLine2(),
                order.getShippingDistrict(),
                order.getShippingCity(),
                order.getShippingStateProvince(),
                order.getShippingPostalCode(),
                order.getShippingCountryCode()
        );
    }

    private BigDecimal safeMoney(BigDecimal value) {
        if (value == null) {
            return ZERO_MONEY;
        }
        return value.setScale(4, RoundingMode.HALF_UP);
    }

    private OrderSummaryDto toSummaryDto(Order order) {
        BigDecimal grandTotal = order.getGrandTotal() == null ? ZERO_MONEY : order.getGrandTotal();
        int itemCount = order.getItems() == null ? 0 : order.getItems().size();
        return new OrderSummaryDto(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus() == null ? null : order.getStatus().name(),
                order.getPlacedAt(),
                order.getCurrencyCode(),
                grandTotal,
                itemCount
        );
    }

    public record CheckoutRequest(Long shippingAddressId,
                                 String currency,
                                 String paymentMethod,
                                 String paymentProvider,
                                 String paymentProviderReference) {
    }

    public record StorefrontOrderItemDto(Long id,
                                        Long productId,
                                        String sku,
                                        String productName,
                                        BigDecimal quantity,
                                        BigDecimal unitPrice,
                                        BigDecimal lineTotal) {
    }

    public record StorefrontOrderShippingAddressDto(String recipientName,
                                                    String phone,
                                                    String line1,
                                                    String line2,
                                                    String district,
                                                    String city,
                                                    String stateProvince,
                                                    String postalCode,
                                                    String countryCode) {
    }

    public record StorefrontOrderResult(Long id,
                                       String orderNumber,
                                       String status,
                                       String currencyCode,
                                       BigDecimal subtotal,
                                       BigDecimal discountTotal,
                                       BigDecimal taxTotal,
                                       BigDecimal shippingTotal,
                                       BigDecimal grandTotal,
                                       LocalDateTime placedAt,
                                       StorefrontOrderShippingAddressDto shippingAddress,
                                       StorefrontPaymentService.PaymentTransactionResult latestPayment,
                                       List<StorefrontOrderItemDto> items) {
    }

    public record OrderSummaryDto(Long id,
                                 String orderNumber,
                                 String status,
                                 LocalDateTime placedAt,
                                 String currencyCode,
                                 BigDecimal grandTotal,
                                 int itemCount) {
    }
}
