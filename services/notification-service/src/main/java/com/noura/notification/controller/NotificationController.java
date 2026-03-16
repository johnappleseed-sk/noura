package com.noura.notification.controller;

import com.noura.notification.common.ApiResponse;
import com.noura.notification.dto.NotificationResponse;
import com.noura.notification.dto.NotificationSendRequest;
import com.noura.notification.service.NotificationMessageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationMessageService notificationMessageService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> listMy(HttpServletRequest request) {
        UUID recipientUserId = resolveUserIdFromRequest(request);
        List<NotificationResponse> notifications = notificationMessageService.listForRecipient(recipientUserId);
        return ResponseEntity.ok(ApiResponse.ok(
                "Notification list loaded",
                notifications,
                request.getRequestURI()
        ));
    }

    @GetMapping("/me/unread-count")
    public ResponseEntity<ApiResponse<Long>> unreadCount(HttpServletRequest request) {
        UUID recipientUserId = resolveUserIdFromRequest(request);
        long count = notificationMessageService.unreadCountForRecipient(recipientUserId);
        return ResponseEntity.ok(ApiResponse.ok(
                "Notification unread count loaded",
                count,
                request.getRequestURI()
        ));
    }

    @PatchMapping("/me/read-all")
    public ResponseEntity<ApiResponse<Integer>> markAllRead(HttpServletRequest request) {
        UUID recipientUserId = resolveUserIdFromRequest(request);
        long updated = notificationMessageService.markAllAsRead(recipientUserId);
        return ResponseEntity.ok(ApiResponse.ok(
                "Notification inbox marked read",
                Math.toIntExact(Math.max(0L, Math.min(Integer.MAX_VALUE, updated))),
                request.getRequestURI()
        ));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markRead(
            @PathVariable UUID notificationId,
            HttpServletRequest request
    ) {
        UUID recipientUserId = resolveUserIdFromRequest(request);
        NotificationResponse response = notificationMessageService.markAsRead(notificationId, recipientUserId);
        return ResponseEntity.ok(ApiResponse.ok(
                "Notification marked read",
                response,
                request.getRequestURI()
        ));
    }

    @PostMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<NotificationResponse>> pushToUser(
            @PathVariable UUID userId,
            @Valid @RequestBody NotificationSendRequest request,
            HttpServletRequest httpRequest
    ) {
        NotificationResponse response = notificationMessageService.createAndDispatch(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                "Notification queued",
                response,
                httpRequest.getRequestURI()
        ));
    }

    @PostMapping("/broadcast")
    public ResponseEntity<ApiResponse<NotificationResponse>> broadcast(
            @Valid @RequestBody NotificationSendRequest request,
            HttpServletRequest requestContext
    ) {
        UUID actorUserId = resolveUserIdFromRequest(requestContext);
        NotificationResponse response = notificationMessageService.createAndDispatch(request, actorUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                "Notification broadcast submitted",
                response,
                requestContext.getRequestURI()
        ));
    }

    private UUID resolveUserIdFromRequest(HttpServletRequest request) {
        String rawSubject = request.getHeader("X-Auth-Subject");
        if (rawSubject == null || rawSubject.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Missing X-Auth-Subject header for authenticated user resolution"
            );
        }

        try {
            return UUID.fromString(rawSubject.trim());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid X-Auth-Subject header value"
            );
        }
    }
}
