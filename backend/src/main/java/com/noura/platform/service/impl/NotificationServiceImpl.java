package com.noura.platform.service.impl;

import com.noura.platform.common.exception.NotFoundException;
import com.noura.platform.config.AppProperties;
import com.noura.platform.domain.entity.Notification;
import com.noura.platform.domain.entity.UserAccount;
import com.noura.platform.dto.notification.NotificationDto;
import com.noura.platform.dto.notification.SendNotificationRequest;
import com.noura.platform.mapper.NotificationMapper;
import com.noura.platform.repository.NotificationRepository;
import com.noura.platform.repository.UserAccountRepository;
import com.noura.platform.security.SecurityUtils;
import com.noura.platform.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserAccountRepository userAccountRepository;
    private final NotificationMapper notificationMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final AppProperties appProperties;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Retrieves my notifications.
     *
     * @return A list of matching items.
     */
    @Override
    public List<NotificationDto> myNotifications() {
        UserAccount user = currentUser();
        return notificationRepository.findTop30ByTargetUserOrderByCreatedAtDesc(user)
                .stream()
                .map(notificationMapper::toDto)
                .toList();
    }

    /**
     * Executes unread count.
     *
     * @return The result of unread count.
     */
    @Override
    public long unreadCount() {
        return notificationRepository.countByTargetUserAndReadFalse(currentUser());
    }

    /**
     * Marks as read.
     *
     * @param notificationId The notification id used to locate the target record.
     * @return The mapped DTO representation.
     */
    @Override
    @Transactional
    public NotificationDto markAsRead(UUID notificationId) {
        UserAccount user = currentUser();
        Notification notification = notificationRepository.findByIdAndTargetUser(notificationId, user)
                .orElseThrow(() -> new NotFoundException("NOTIFICATION_NOT_FOUND", "Notification not found"));
        if (!notification.isRead()) {
            notification.setRead(true);
            notification = notificationRepository.save(notification);
        }
        return notificationMapper.toDto(notification);
    }

    /**
     * Marks all as read.
     *
     * @return The number of updated records.
     */
    @Override
    @Transactional
    public int markAllAsRead() {
        UserAccount user = currentUser();
        List<Notification> unread = notificationRepository.findByTargetUserAndReadFalse(user);
        if (unread.isEmpty()) {
            return 0;
        }
        unread.forEach(notification -> notification.setRead(true));
        notificationRepository.saveAll(unread);
        return unread.size();
    }

    /**
     * Pushes to user.
     *
     * @param targetUserId The target user id used to locate the target record.
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public NotificationDto pushToUser(UUID targetUserId, SendNotificationRequest request) {
        UserAccount target = userAccountRepository.findById(targetUserId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "Target user not found"));
        Notification notification = new Notification();
        notification.setTargetUser(target);
        notification.setCategory(request.category());
        notification.setTitle(request.title());
        notification.setBody(request.body());
        notification.setRead(false);
        Notification saved = notificationRepository.save(notification);
        NotificationDto dto = notificationMapper.toDto(saved);
        redisTemplate.convertAndSend(appProperties.getNotifications().getRedisChannel(), dto);
        messagingTemplate.convertAndSendToUser(target.getEmail(), "/queue/notifications", dto);
        return dto;
    }

    /**
     * Broadcasts message.
     *
     * @param request The request payload for this operation.
     */
    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void broadcast(SendNotificationRequest request) {
        NotificationDto dto = new NotificationDto(
                UUID.randomUUID(),
                null,
                request.category(),
                request.title(),
                request.body(),
                false,
                java.time.Instant.now()
        );
        redisTemplate.convertAndSend(appProperties.getNotifications().getRedisChannel(), dto);
        messagingTemplate.convertAndSend("/topic/notifications", dto);
    }

    /**
     * Executes current user.
     *
     * @return The result of current user.
     */
    private UserAccount currentUser() {
        return userAccountRepository.findByEmailIgnoreCase(SecurityUtils.currentEmail())
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
    }
}
