package com.noura.platform.repository;

import com.noura.platform.domain.entity.Notification;
import com.noura.platform.domain.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    /**
     * Finds top30 by target user order by created at desc.
     *
     * @param user The user context for this operation.
     * @return A list of matching items.
     */
    List<Notification> findTop30ByTargetUserOrderByCreatedAtDesc(UserAccount user);

    /**
     * Finds by id and target user.
     *
     * @param id The id used to locate the target record.
     * @param user The user context for this operation.
     * @return The result of find by id and target user.
     */
    Optional<Notification> findByIdAndTargetUser(UUID id, UserAccount user);

    /**
     * Finds by target user and read false.
     *
     * @param user The user context for this operation.
     * @return A list of matching items.
     */
    List<Notification> findByTargetUserAndReadFalse(UserAccount user);

    /**
     * Counts by target user and read false.
     *
     * @param user The user context for this operation.
     * @return The result of counts by target user and read false.
     */
    long countByTargetUserAndReadFalse(UserAccount user);
}
