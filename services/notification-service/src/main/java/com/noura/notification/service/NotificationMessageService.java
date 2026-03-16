package com.noura.notification.service;

import com.noura.notification.domain.NotificationMessage;
import com.noura.notification.domain.enums.NotificationChannel;
import com.noura.notification.domain.enums.NotificationStatus;
import com.noura.notification.domain.enums.NotificationCategory;
import com.noura.notification.domain.enums.NotificationType;
import com.noura.notification.dto.InternalNotificationCommandRequest;
import com.noura.notification.dto.NotificationResponse;
import com.noura.notification.dto.NotificationDispatchResponse;
import com.noura.notification.dto.NotificationSendRequest;
import com.noura.notification.repository.NotificationMessageRepository;
import com.noura.notification.service.dispatcher.NotificationDispatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationMessageService {

    private final NotificationMessageRepository notificationMessageRepository;
    private final List<NotificationDispatcher> dispatchers;

    @Transactional
    public NotificationDispatchResponse createAndDispatch(InternalNotificationCommandRequest request) {
        NotificationMessage saved = createAndDispatchInternal(
                request.targetUserId(),
                request.category(),
                request.title(),
                request.body()
        );
        return toResponse(saved);
    }

    @Transactional
    public NotificationResponse createAndDispatch(NotificationSendRequest request, UUID targetUserId) {
        NotificationMessage saved = createAndDispatchInternal(
                targetUserId,
                request.category(),
                request.title(),
                request.body()
        );
        return toNotificationResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> listForRecipient(UUID recipientUserId) {
        return notificationMessageRepository.findByRecipientUserIdOrderByCreatedAtDesc(recipientUserId)
                .stream()
                .map(this::toNotificationResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public long unreadCountForRecipient(UUID recipientUserId) {
        return notificationMessageRepository.countByRecipientUserIdAndStatusIn(
                recipientUserId,
                List.of(NotificationStatus.PENDING, NotificationStatus.SENT)
        );
    }

    @Transactional
    public NotificationResponse markAsRead(UUID notificationId, UUID recipientUserId) {
        NotificationMessage message = notificationMessageRepository.findByIdAndRecipientUserId(notificationId, recipientUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));

        if (message.getStatus() == NotificationStatus.READ) {
            return toNotificationResponse(message);
        }

        message.setStatus(NotificationStatus.READ);
        return toNotificationResponse(notificationMessageRepository.save(message));
    }

    @Transactional
    public long markAllAsRead(UUID recipientUserId) {
        List<NotificationMessage> messages = notificationMessageRepository
                .findByRecipientUserIdOrderByCreatedAtDesc(recipientUserId)
                .stream()
                .filter(message -> message.getStatus() != NotificationStatus.READ)
                .toList();

        if (messages.isEmpty()) {
            return 0L;
        }

        for (NotificationMessage message : messages) {
            message.setStatus(NotificationStatus.READ);
        }
        notificationMessageRepository.saveAll(messages);
        return messages.size();
    }

    private NotificationMessage createAndDispatchInternal(
            UUID targetUserId,
            NotificationCategory category,
            String title,
            String body
    ) {
        NotificationMessage message = new NotificationMessage();
        message.setRecipientUserId(targetUserId);
        message.setType(resolveType(category));
        message.setChannel(NotificationChannel.IN_APP);
        message.setSubject(title);
        message.setBody(body);
        message.setStatus(NotificationStatus.PENDING);
        message.setSentAt(null);
        message.setFailedAt(null);
        message.setFailureReason(null);

        NotificationMessage saved = notificationMessageRepository.save(message);
        NotificationDispatcher dispatcher = resolveDispatcher(saved.getChannel());
        if (dispatcher == null) {
            saved.setStatus(NotificationStatus.FAILED);
            saved.setFailedAt(Instant.now());
            saved.setFailureReason("No dispatcher configured for channel " + saved.getChannel());
            saved = notificationMessageRepository.save(saved);
            return saved;
        }

        NotificationDispatcher.DispatchResult dispatchResult = dispatcher.dispatch(saved);
        saved.setStatus(dispatchResult.status());
        saved.setSentAt(dispatchResult.sentAt());
        saved.setFailedAt(dispatchResult.failedAt());
        saved.setFailureReason(trimToNull(dispatchResult.failureReason()));
        saved = notificationMessageRepository.save(saved);
        return saved;
    }

    private NotificationDispatcher resolveDispatcher(NotificationChannel channel) {
        return dispatchers.stream()
                .filter(dispatcher -> dispatcher.supports(channel))
                .findFirst()
                .orElse(null);
    }

    private NotificationType resolveType(NotificationCategory category) {
        return switch (category) {
            case ORDER -> NotificationType.ORDER;
            case STORE -> NotificationType.STORE;
            case SECURITY -> NotificationType.SECURITY;
            case SYSTEM -> NotificationType.SYSTEM;
            case AI -> NotificationType.AI;
        };
    }

    private NotificationDispatchResponse toResponse(NotificationMessage message) {
        return new NotificationDispatchResponse(
                message.getId(),
                message.getRecipientUserId(),
                message.getType(),
                message.getChannel(),
                message.getStatus(),
                message.getCreatedAt(),
                message.getSentAt(),
                message.getFailedAt(),
                message.getFailureReason()
        );
    }

    private NotificationResponse toNotificationResponse(NotificationMessage message) {
        return new NotificationResponse(
                message.getId(),
                message.getRecipientUserId(),
                mapTypeToCategory(message.getType()),
                message.getSubject(),
                message.getBody(),
                isRead(message.getStatus()),
                message.getCreatedAt()
        );
    }

    private NotificationCategory mapTypeToCategory(NotificationType type) {
        return switch (type) {
            case ORDER -> NotificationCategory.ORDER;
            case STORE -> NotificationCategory.STORE;
            case SECURITY -> NotificationCategory.SECURITY;
            case SYSTEM -> NotificationCategory.SYSTEM;
            case AI -> NotificationCategory.AI;
            default -> NotificationCategory.SYSTEM;
        };
    }

    private boolean isRead(NotificationStatus status) {
        return status == NotificationStatus.READ;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
