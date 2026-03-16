package com.noura.platform.service.impl;

import com.noura.platform.common.exception.BadRequestException;
import com.noura.platform.common.exception.NotFoundException;
import com.noura.platform.domain.entity.NotificationMessage;
import com.noura.platform.domain.entity.UserAccount;
import com.noura.platform.domain.enums.NotificationChannel;
import com.noura.platform.domain.enums.NotificationStatus;
import com.noura.platform.domain.enums.NotificationType;
import com.noura.platform.dto.notification.CreateNotificationRequest;
import com.noura.platform.dto.notification.NotificationResponse;
import com.noura.platform.dto.notification.SendNotificationRequest;
import com.noura.platform.repository.NotificationRepository;
import com.noura.platform.repository.UserAccountRepository;
import com.noura.platform.security.SecurityUtils;
import com.noura.platform.service.NotificationService;
import com.noura.platform.service.notification.NotificationDispatcher;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserAccountRepository userAccountRepository;
    private final List<NotificationDispatcher> dispatchers;

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public NotificationResponse createNotification(CreateNotificationRequest request) {
        return createAndDispatch(request);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public Page<NotificationResponse> listNotifications(NotificationStatus status, UUID recipientUserId, Pageable pageable) {
        return notificationRepository.findAll(buildSpecification(status, recipientUserId), pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Page<NotificationResponse> myNotifications(NotificationStatus status, Pageable pageable) {
        UserAccount currentUser = currentUser();
        return notificationRepository.findAll(buildSpecification(status, currentUser.getId()), pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional
    public NotificationResponse pushToUser(UUID targetUserId, SendNotificationRequest request) {
        CreateNotificationRequest mappedRequest = new CreateNotificationRequest(
                targetUserId,
                NotificationType.fromCategory(request == null ? null : request.category()),
                NotificationChannel.IN_APP,
                request == null ? null : request.title(),
                request == null ? null : request.body(),
                null,
                null
        );
        return createAndDispatch(mappedRequest);
    }

    private NotificationResponse createAndDispatch(CreateNotificationRequest request) {
        UserAccount recipient = userAccountRepository.findById(request.recipientUserId())
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "Recipient user not found"));

        NotificationMessage message = new NotificationMessage();
        message.setRecipientUser(recipient);
        message.setType(request.type());
        message.setChannel(request.channel());
        message.setSubject(request.subject());
        message.setBody(request.body());
        message.setStatus(NotificationStatus.PENDING);
        message.setRelatedEntityType(trimToNull(request.relatedEntityType()));
        message.setRelatedEntityId(request.relatedEntityId());

        NotificationMessage saved = notificationRepository.save(message);
        NotificationDispatcher dispatcher = resolveDispatcher(saved.getChannel());
        if (dispatcher == null) {
            saved.setStatus(NotificationStatus.FAILED);
            saved.setFailureReason("No dispatcher configured for channel " + saved.getChannel());
            saved = notificationRepository.save(saved);
            return toResponse(saved);
        }

        NotificationDispatcher.DispatchResult result = dispatcher.dispatch(saved);
        saved.setStatus(result.status());
        saved.setSentAt(result.sentAt());
        saved.setFailedAt(result.failedAt());
        saved.setFailureReason(trimToNull(result.failureReason()));
        saved = notificationRepository.save(saved);
        return toResponse(saved);
    }

    private NotificationDispatcher resolveDispatcher(NotificationChannel channel) {
        return dispatchers.stream()
                .filter(dispatcher -> dispatcher.supports(channel))
                .findFirst()
                .orElse(null);
    }

    private Specification<NotificationMessage> buildSpecification(NotificationStatus status, UUID recipientUserId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (recipientUserId != null) {
                predicates.add(cb.equal(root.get("recipientUserId"), recipientUserId));
            }
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private UserAccount currentUser() {
        return userAccountRepository.findByEmailIgnoreCase(SecurityUtils.currentEmail())
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
    }

    private NotificationResponse toResponse(NotificationMessage message) {
        return new NotificationResponse(
                message.getId(),
                message.getRecipientUserId(),
                message.getType(),
                message.getChannel(),
                message.getSubject(),
                message.getBody(),
                message.getStatus(),
                message.getRelatedEntityType(),
                message.getRelatedEntityId(),
                message.getCreatedAt(),
                message.getSentAt(),
                message.getFailedAt(),
                message.getFailureReason()
        );
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
