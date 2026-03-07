package com.noura.platform.repository;

import com.noura.platform.domain.entity.Cart;
import com.noura.platform.domain.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CartRepository extends JpaRepository<Cart, UUID> {
    /**
     * Finds by user.
     *
     * @param user The user context for this operation.
     * @return The result of find by user.
     */
    Optional<Cart> findByUser(UserAccount user);
}
