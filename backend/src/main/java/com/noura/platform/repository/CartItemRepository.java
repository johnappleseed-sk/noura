package com.noura.platform.repository;

import com.noura.platform.domain.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CartItemRepository extends JpaRepository<CartItem, UUID> {
    /**
     * Finds by cart id.
     *
     * @param cartId The cart id used to locate the target record.
     * @return A list of matching items.
     */
    List<CartItem> findByCartId(UUID cartId);

    /**
     * Finds by cart id and product id.
     *
     * @param cartId The cart id used to locate the target record.
     * @param productId The product id used to locate the target record.
     * @return The result of find by cart id and product id.
     */
    Optional<CartItem> findByCartIdAndProductId(UUID cartId, UUID productId);

    /**
     * Deletes by cart id.
     *
     * @param cartId The cart id used to locate the target record.
     */
    void deleteByCartId(UUID cartId);
}
