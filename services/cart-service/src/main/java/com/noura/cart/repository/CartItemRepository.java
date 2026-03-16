package com.noura.cart.repository;

import com.noura.cart.domain.entity.CartItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence gateway for cart line items.
 */
public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    /**
     * Lists cart items ordered by creation time.
     *
     * @param cartId cart identifier
     * @return ordered cart items
     */
    List<CartItem> findByCartIdOrderByCreatedAtAsc(UUID cartId);

    /**
     * Loads one cart item scoped to a cart aggregate.
     *
     * @param itemId item identifier
     * @param cartId cart identifier
     * @return matching item when found
     */
    Optional<CartItem> findByIdAndCartId(UUID itemId, UUID cartId);

    /**
     * Loads a matching deduplicated line with pessimistic write lock.
     *
     * @param cartId cart identifier
     * @param productId product identifier
     * @param variantId optional variant identifier
     * @param storeId optional store identifier
     * @return matching line item when found
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select item
            from CartItem item
            where item.cartId = :cartId
              and item.productId = :productId
              and ((:variantId is null and item.variantId is null) or item.variantId = :variantId)
              and ((:storeId is null and item.storeId is null) or item.storeId = :storeId)
            """)
    Optional<CartItem> findDeduplicatedItemForUpdate(
            @Param("cartId") UUID cartId,
            @Param("productId") UUID productId,
            @Param("variantId") UUID variantId,
            @Param("storeId") UUID storeId
    );

    /**
     * Deletes all line items for a cart aggregate.
     *
     * @param cartId cart identifier
     */
    void deleteByCartId(UUID cartId);
}
