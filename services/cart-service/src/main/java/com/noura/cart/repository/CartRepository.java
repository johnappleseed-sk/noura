package com.noura.cart.repository;

import com.noura.cart.domain.entity.Cart;
import com.noura.cart.domain.enums.CartOwnerType;
import com.noura.cart.domain.enums.CartStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Persistence gateway for cart aggregate roots.
 */
public interface CartRepository extends JpaRepository<Cart, UUID> {

    /**
     * Loads the active customer cart without lock.
     *
     * @param ownerType owner type
     * @param customerId customer identifier
     * @param status cart status
     * @return active customer cart when found
     */
    Optional<Cart> findByOwnerTypeAndCustomerIdAndStatus(CartOwnerType ownerType, String customerId, CartStatus status);

    /**
     * Loads the active guest cart without lock.
     *
     * @param ownerType owner type
     * @param guestToken guest cart token
     * @param status cart status
     * @return active guest cart when found
     */
    Optional<Cart> findByOwnerTypeAndGuestTokenAndStatus(CartOwnerType ownerType, String guestToken, CartStatus status);

    /**
     * Loads the active customer cart with pessimistic write lock.
     *
     * @param customerId customer identifier
     * @return locked customer cart when found
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select cart
            from Cart cart
            where cart.ownerType = com.noura.cart.domain.enums.CartOwnerType.CUSTOMER
              and cart.status = com.noura.cart.domain.enums.CartStatus.ACTIVE
              and cart.customerId = :customerId
            """)
    Optional<Cart> findActiveCustomerCartForUpdate(@Param("customerId") String customerId);

    /**
     * Loads the active guest cart with pessimistic write lock.
     *
     * @param guestToken guest cart token
     * @return locked guest cart when found
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select cart
            from Cart cart
            where cart.ownerType = com.noura.cart.domain.enums.CartOwnerType.GUEST
              and cart.status = com.noura.cart.domain.enums.CartStatus.ACTIVE
              and cart.guestToken = :guestToken
            """)
    Optional<Cart> findActiveGuestCartForUpdate(@Param("guestToken") String guestToken);
}
