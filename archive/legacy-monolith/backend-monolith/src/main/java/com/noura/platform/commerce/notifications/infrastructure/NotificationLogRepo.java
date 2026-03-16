package com.noura.platform.commerce.notifications.infrastructure;

import com.noura.platform.commerce.notifications.domain.NotificationLog;
import com.noura.platform.commerce.notifications.domain.NotificationStatus;
import com.noura.platform.commerce.notifications.domain.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationLogRepo extends JpaRepository<NotificationLog, Long> {

    List<NotificationLog> findByReferenceTypeAndReferenceIdOrderByCreatedAtDesc(String referenceType, Long referenceId);

    List<NotificationLog> findByRecipientOrderByCreatedAtDesc(String recipient);

    Page<NotificationLog> findByStatus(NotificationStatus status, Pageable pageable);

    Page<NotificationLog> findByType(NotificationType type, Pageable pageable);

    List<NotificationLog> findByStatusAndRetryCountLessThanOrderByCreatedAtAsc(NotificationStatus status, int maxRetries);

    long countByReferenceTypeAndReferenceIdAndType(String referenceType, Long referenceId, NotificationType type);
}
