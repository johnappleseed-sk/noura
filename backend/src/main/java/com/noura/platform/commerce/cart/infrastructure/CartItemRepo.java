package com.noura.platform.commerce.cart.infrastructure;

import com.noura.platform.commerce.cart.domain.CartItem;
import com.noura.platform.commerce.cart.domain.CartStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartItemRepo extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findByIdAndCart_IdAndCart_Status(Long id, Long cartId, CartStatus status);

    Optional<CartItem> findByCart_IdAndProductId(Long cartId, Long productId);

    void deleteAllByCart_IdAndCart_Status(Long cartId, CartStatus status);
}
