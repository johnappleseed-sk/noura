package com.noura.platform.repository;

import com.noura.platform.domain.entity.PaymentMethod;
import com.noura.platform.domain.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, UUID> {
    /**
     * Finds by user.
     *
     * @param user The user context for this operation.
     * @return A list of matching items.
     */
    List<PaymentMethod> findByUser(UserAccount user);

    /**
     * Finds by id and user.
     *
     * @param id The id used to locate the target record.
     * @param user The user context for this operation.
     * @return The result of find by id and user.
     */
    Optional<PaymentMethod> findByIdAndUser(UUID id, UserAccount user);
}
