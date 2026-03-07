package com.noura.platform.controller;

import com.noura.platform.common.api.ApiResponse;
import com.noura.platform.dto.notification.NotificationDto;
import com.noura.platform.dto.notification.SendNotificationRequest;
import com.noura.platform.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("${app.api.version-prefix:/api/v1}/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Retrieves my notifications.
     *
     * @param http The current HTTP request used to populate response metadata.
     * @return A list of matching items.
     */
    @GetMapping("/me")
    public ApiResponse<List<NotificationDto>> myNotifications(HttpServletRequest http) {
        return ApiResponse.ok("Notifications", notificationService.myNotifications(), http.getRequestURI());
    }

    /**
     * Executes unread count.
     *
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @GetMapping("/me/unread-count")
    public ApiResponse<Long> unreadCount(HttpServletRequest http) {
        return ApiResponse.ok("Unread count", notificationService.unreadCount(), http.getRequestURI());
    }

    /**
     * Marks as read.
     *
     * @param notificationId The notification id used to locate the target record.
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @PatchMapping("/{notificationId}/read")
    public ApiResponse<NotificationDto> markAsRead(@PathVariable UUID notificationId, HttpServletRequest http) {
        return ApiResponse.ok("Notification marked as read", notificationService.markAsRead(notificationId), http.getRequestURI());
    }

    /**
     * Marks all as read.
     *
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @PatchMapping("/me/read-all")
    public ApiResponse<Integer> markAllAsRead(HttpServletRequest http) {
        return ApiResponse.ok("All notifications marked as read", notificationService.markAllAsRead(), http.getRequestURI());
    }

    /**
     * Pushes to user.
     *
     * @param userId The user id used to locate the target record.
     * @param request The request payload for this operation.
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @PostMapping("/user/{userId}")
    public ApiResponse<NotificationDto> pushToUser(
            @PathVariable UUID userId,
            @Valid @RequestBody SendNotificationRequest request,
            HttpServletRequest http
    ) {
        return ApiResponse.ok("Notification pushed", notificationService.pushToUser(userId, request), http.getRequestURI());
    }

    /**
     * Broadcasts message.
     *
     * @param request The request payload for this operation.
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @PostMapping("/broadcast")
    public ApiResponse<Void> broadcast(@Valid @RequestBody SendNotificationRequest request, HttpServletRequest http) {
        notificationService.broadcast(request);
        return ApiResponse.ok("Broadcast sent", null, http.getRequestURI());
    }
}
