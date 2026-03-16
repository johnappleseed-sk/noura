package com.noura.platform.commerce.cart.application;

import com.noura.platform.commerce.entity.Product;
import com.noura.platform.commerce.cart.domain.Cart;
import com.noura.platform.commerce.cart.domain.CartItem;
import com.noura.platform.commerce.cart.domain.CartStatus;
import com.noura.platform.commerce.cart.infrastructure.CartItemRepo;
import com.noura.platform.commerce.cart.infrastructure.CartRepo;
import com.noura.platform.commerce.customers.domain.CustomerAccount;
import com.noura.platform.commerce.customers.infrastructure.CustomerAccountRepo;
import com.noura.platform.commerce.orders.domain.Order;
import com.noura.platform.commerce.orders.domain.OrderItem;
import com.noura.platform.commerce.orders.domain.OrderStatus;
import com.noura.platform.commerce.orders.infrastructure.OrderRepo;
import com.noura.platform.commerce.repository.ProductRepo;
import com.noura.platform.dto.storefront.StorefrontAddCartItemRequest;
import com.noura.platform.dto.storefront.StorefrontCartDto;
import com.noura.platform.dto.storefront.StorefrontCartItemDto;
import com.noura.platform.dto.storefront.StorefrontUpdateCartItemRequest;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class StorefrontCartService {
    private final CartRepo cartRepo;
    private final CartItemRepo cartItemRepo;
    private final CustomerAccountRepo customerAccountRepo;
    private final ProductRepo productRepo;
    private final OrderRepo orderRepo;

    public StorefrontCartService(CartRepo cartRepo,
                                CartItemRepo cartItemRepo,
                                CustomerAccountRepo customerAccountRepo,
                                ProductRepo productRepo,
                                OrderRepo orderRepo) {
        this.cartRepo = cartRepo;
        this.cartItemRepo = cartItemRepo;
        this.customerAccountRepo = customerAccountRepo;
        this.productRepo = productRepo;
        this.orderRepo = orderRepo;
    }

    @Transactional
    public StorefrontCartDto getOrCreateCart(Long customerId) {
        Cart cart = resolveActiveCartByCustomer(customerId);
        return toDto(cart);
    }

    @Transactional
    public StorefrontCartDto addItem(Long customerId, StorefrontAddCartItemRequest request) {
        long productId = request.productId();
        int quantity = request.quantity();
        if (productId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valid productId is required.");
        }
        if (quantity <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be greater than zero.");
        }

        Cart cart = resolveActiveCartByCustomer(customerId);
        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found."));
        if (!Boolean.TRUE.equals(product.getActive())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product is not available.");
        }

        BigDecimal unitPrice = product.getPrice() == null ? BigDecimal.ZERO : product.getPrice();
        boolean[] isNew = new boolean[]{false};
        CartItem item = cartItemRepo.findByCart_IdAndProductId(cart.getId(), productId)
                .orElseGet(() -> {
                    isNew[0] = true;
                    CartItem created = new CartItem();
                    created.setCart(cart);
                    created.setProductId(productId);
                    created.setSku(product.getSku());
                    created.setProductName(product.getName());
                    created.setUnitLabel(product.getBaseUnitName());
                    created.setUnitPrice(unitPrice.setScale(4, RoundingMode.HALF_UP));
                    return created;
                });
        item.setQuantity((item.getQuantity() == null ? 0 : item.getQuantity()) + quantity);
        item.setLineTotal(computeLineTotal(item.getUnitPrice(), item.getQuantity()));
        if (isNew[0]) {
            cart.getItems().add(item);
        }

        Cart saved = cartRepo.save(cart);
        return toDto(saved);
    }

    @Transactional
    public StorefrontCartDto updateItem(Long customerId, Long itemId, StorefrontUpdateCartItemRequest request) {
        if (itemId == null || itemId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid item id.");
        }
        int quantity = request.quantity();
        if (quantity < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be at least 1.");
        }

        Cart cart = resolveActiveCartByCustomer(customerId);
        CartItem item = cartItemRepo.findByIdAndCart_IdAndCart_Status(itemId, cart.getId(), CartStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart item not found."));
        item.setQuantity(quantity);
        item.setLineTotal(computeLineTotal(item.getUnitPrice(), quantity));
        return toDto(cartRepo.save(cart));
    }

    @Transactional
    public void removeItem(Long customerId, Long itemId) {
        if (itemId == null || itemId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid item id.");
        }
        Cart cart = resolveActiveCartByCustomer(customerId);
        CartItem item = cartItemRepo.findByIdAndCart_IdAndCart_Status(itemId, cart.getId(), CartStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart item not found."));
        cart.getItems().remove(item);
        cartItemRepo.delete(item);
    }

    @Transactional
    public StorefrontCartDto clearCart(Long customerId) {
        Cart cart = resolveActiveCartByCustomer(customerId);
        cartItemRepo.deleteAllByCart_IdAndCart_Status(cart.getId(), CartStatus.ACTIVE);
        cart.getItems().clear();
        return toDto(cart);
    }

    public Cart resolveActiveCartForCheckout(Long customerId) {
        return resolveActiveCartByCustomer(customerId);
    }

    public List<Order> findCustomerOrders(Long customerId) {
        return orderRepo.findByCustomerAccount_IdOrderByPlacedAtDesc(customerId);
    }

    public StorefrontCartDto mapToDto(Cart cart) {
        return toDto(cart);
    }

    public void removeActiveItemsAfterCheckout(Cart cart) {
        if (cart != null) {
            cartItemRepo.deleteAllByCart_IdAndCart_Status(cart.getId(), CartStatus.ACTIVE);
            cart.getItems().clear();
        }
    }

    private Cart resolveActiveCartByCustomer(Long customerId) {
        CustomerAccount customer = customerAccountRepo.findById(customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Customer not found."));

        return cartRepo.findByCustomerAccount_IdAndStatus(customer.getId(), CartStatus.ACTIVE)
                .orElseGet(() -> {
                    Cart cart = new Cart();
                    cart.setCustomerAccount(customer);
                    cart.setStatus(CartStatus.ACTIVE);
                    return cartRepo.save(cart);
                });
    }

    private BigDecimal computeLineTotal(BigDecimal unitPrice, int quantity) {
        BigDecimal safePrice = unitPrice == null ? BigDecimal.ZERO : unitPrice;
        return safePrice.multiply(BigDecimal.valueOf(quantity))
                .setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal computeLineTotal(BigDecimal unitPrice, Integer quantity) {
        return computeLineTotal(unitPrice, quantity == null ? 0 : quantity);
    }

    private StorefrontCartDto toDto(Cart cart) {
        List<StorefrontCartItemDto> items = cart.getItems() == null ? List.of() : cart.getItems().stream()
                .map(item -> new StorefrontCartItemDto(
                        item.getId(),
                        item.getProductId(),
                        item.getSku(),
                        item.getProductName(),
                        item.getUnitLabel(),
                        item.getQuantity() == null ? 0 : item.getQuantity(),
                        item.getUnitPrice(),
                        item.getLineTotal()
                ))
                .toList();

        BigDecimal subtotal = items.stream()
                .map(StorefrontCartItemDto::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(4, RoundingMode.HALF_UP);

        return new StorefrontCartDto(
                cart.getId(),
                cart.getStatus().name(),
                cart.getCurrencyCode(),
                items,
                subtotal,
                items.size(),
                cart.getUpdatedAt()
        );
    }

}
