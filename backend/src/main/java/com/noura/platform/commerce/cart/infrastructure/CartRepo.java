package com.noura.platform.commerce.cart.infrastructure;

import com.noura.platform.commerce.cart.domain.Cart;
import com.noura.platform.commerce.cart.domain.CartStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepo extends JpaRepository<Cart, Long> {
    @EntityGraph(attributePaths = "items")
    Optional<Cart> findByCustomerAccount_IdAndStatus(Long customerAccountId, CartStatus status);

    @EntityGraph(attributePaths = "items")
    Optional<Cart> findByIdAndCustomerAccount_IdAndStatus(Long cartId, Long customerAccountId, CartStatus status);
}
