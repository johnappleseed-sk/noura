package com.noura.platform.controller;

import com.noura.platform.common.api.ApiResponse;
import com.noura.platform.common.api.PageResponse;
import com.noura.platform.common.api.PaginationUtils;
import com.noura.platform.domain.enums.NotificationStatus;
import com.noura.platform.dto.notification.CreateNotificationRequest;
import com.noura.platform.dto.notification.NotificationResponse;
import com.noura.platform.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Validated
@RestController
@RequiredArgsConstructor
public class NotificationAdminController {

    private final NotificationService notificationService;

    @PostMapping({"/api/admin/notifications", "${app.api.version-prefix:/api/v1}/admin/notifications"})
    public ResponseEntity<ApiResponse<NotificationResponse>> create(
            @Valid @RequestBody CreateNotificationRequest request,
            HttpServletRequest http
    ) {
        NotificationResponse response = notificationService.createNotification(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Notification created", response, http.getRequestURI()));
    }

    @GetMapping({"/api/admin/notifications", "${app.api.version-prefix:/api/v1}/admin/notifications"})
    public ApiResponse<PageResponse<NotificationResponse>> listAdmin(
            @RequestParam(required = false) NotificationStatus status,
            @RequestParam(required = false) UUID recipientUserId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            HttpServletRequest http
    ) {
        Pageable pageable = PaginationUtils.pageOf(page, size, sortBy, direction);
        Page<NotificationResponse> response = notificationService.listNotifications(status, recipientUserId, pageable);
        return ApiResponse.ok("Notifications", PageResponse.from(response), http.getRequestURI());
    }

    @GetMapping({"/api/my/notifications", "${app.api.version-prefix:/api/v1}/my/notifications"})
    public ApiResponse<PageResponse<NotificationResponse>> listMine(
            @RequestParam(required = false) NotificationStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            HttpServletRequest http
    ) {
        Pageable pageable = PaginationUtils.pageOf(page, size, sortBy, direction);
        Page<NotificationResponse> response = notificationService.myNotifications(status, pageable);
        return ApiResponse.ok("My notifications", PageResponse.from(response), http.getRequestURI());
    }
}
