package com.noura.notification.repository;

import com.noura.notification.domain.NotificationMessage;
import com.noura.notification.domain.enums.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationMessageRepository extends JpaRepository<NotificationMessage, UUID> {
    List<NotificationMessage> findByRecipientUserIdOrderByCreatedAtDesc(UUID recipientUserId);

    long countByRecipientUserIdAndStatusIn(UUID recipientUserId, List<NotificationStatus> statuses);

    Optional<NotificationMessage> findByIdAndRecipientUserId(UUID id, UUID recipientUserId);
}
