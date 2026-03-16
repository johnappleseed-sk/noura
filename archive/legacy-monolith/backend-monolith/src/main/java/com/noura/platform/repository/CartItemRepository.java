package com.noura.platform.repository;

import com.noura.platform.domain.entity.CartItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
    @EntityGraph(attributePaths = {"product", "cart", "cart.store", "storeProductReference", "storeProductReference.store"})
    List<CartItem> findByCartId(UUID cartId);

    @EntityGraph(attributePaths = {"product", "cart", "cart.store", "storeProductReference", "storeProductReference.store"})
    List<CartItem> findByCartIdOrderByCreatedAtAsc(UUID cartId);

    /**
     * Finds by cart id and product id.
     *
     * @param cartId The cart id used to locate the target record.
     * @param productId The product id used to locate the target record.
     * @return The result of find by cart id and product id.
     */
    Optional<CartItem> findByCartIdAndProductId(UUID cartId, UUID productId);

    Optional<CartItem> findByCartIdAndStoreProductReferenceId(UUID cartId, UUID storeProductReferenceId);

    @EntityGraph(attributePaths = {"product", "cart", "cart.store", "storeProductReference", "storeProductReference.store"})
    @Query("""
            select item
            from CartItem item
            join item.cart cart
            join cart.user user
            where item.id = :itemId
              and user.id = :userId
            """)
    Optional<CartItem> findOwnedItemById(@Param("itemId") UUID itemId, @Param("userId") UUID userId);

    /**
     * Deletes by cart id.
     *
     * @param cartId The cart id used to locate the target record.
     */
    void deleteByCartId(UUID cartId);
}
