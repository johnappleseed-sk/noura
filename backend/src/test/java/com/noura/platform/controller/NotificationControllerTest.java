package com.noura.platform.controller;

import com.noura.platform.common.api.ApiResponse;
import com.noura.platform.domain.enums.NotificationCategory;
import com.noura.platform.dto.notification.NotificationDto;
import com.noura.platform.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @Test
    void unreadCount_shouldReturnCounter() {
        NotificationController controller = new NotificationController(notificationService);
        when(notificationService.unreadCount()).thenReturn(7L);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/notifications/me/unread-count");

        ApiResponse<Long> response = controller.unreadCount(request);

        assertEquals("Unread count", response.getMessage());
        assertEquals(7L, response.getData());
        verify(notificationService, times(1)).unreadCount();
    }

    @Test
    void markAsRead_shouldReturnNotification() {
        NotificationController controller = new NotificationController(notificationService);
        UUID id = UUID.randomUUID();
        NotificationDto dto = new NotificationDto(
                id,
                UUID.randomUUID(),
                NotificationCategory.ORDER,
                "Order status updated",
                "Your order has shipped.",
                true,
                Instant.now()
        );
        when(notificationService.markAsRead(id)).thenReturn(dto);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/notifications/" + id + "/read");

        ApiResponse<NotificationDto> response = controller.markAsRead(id, request);

        assertEquals("Notification marked as read", response.getMessage());
        assertSame(dto, response.getData());
        verify(notificationService, times(1)).markAsRead(id);
    }

    @Test
    void markAllAsRead_shouldReturnUpdatedCount() {
        NotificationController controller = new NotificationController(notificationService);
        when(notificationService.markAllAsRead()).thenReturn(3);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/notifications/me/read-all");

        ApiResponse<Integer> response = controller.markAllAsRead(request);

        assertEquals("All notifications marked as read", response.getMessage());
        assertEquals(3, response.getData());
        verify(notificationService, times(1)).markAllAsRead();
    }
}

