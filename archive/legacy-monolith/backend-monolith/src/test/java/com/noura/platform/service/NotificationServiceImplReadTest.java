package com.noura.platform.service;

import com.noura.platform.common.exception.NotFoundException;
import com.noura.platform.config.AppProperties;
import com.noura.platform.domain.entity.Notification;
import com.noura.platform.domain.entity.UserAccount;
import com.noura.platform.mapper.NotificationMapper;
import com.noura.platform.repository.NotificationRepository;
import com.noura.platform.repository.UserAccountRepository;
import com.noura.platform.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplReadTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private NotificationMapper notificationMapper;

    private NotificationServiceImpl notificationService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "customer@noura.test",
                        "n/a",
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                )
        );
        AppProperties appProperties = new AppProperties();
        notificationService = new NotificationServiceImpl(
                notificationRepository,
                userAccountRepository,
                notificationMapper,
                null,
                appProperties,
                null
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void markAllAsRead_shouldUpdateUnreadNotifications() {
        UserAccount user = new UserAccount();
        user.setEmail("customer@noura.test");

        Notification first = new Notification();
        first.setRead(false);
        Notification second = new Notification();
        second.setRead(false);

        when(userAccountRepository.findByEmailIgnoreCase("customer@noura.test")).thenReturn(Optional.of(user));
        when(notificationRepository.findByTargetUserAndReadFalse(user)).thenReturn(List.of(first, second));

        int updated = notificationService.markAllAsRead();

        assertEquals(2, updated);
        assertTrue(first.isRead());
        assertTrue(second.isRead());
        verify(notificationRepository).saveAll(anyList());
    }

    @Test
    void markAllAsRead_shouldSkipSaveWhenNothingToUpdate() {
        UserAccount user = new UserAccount();
        user.setEmail("customer@noura.test");

        when(userAccountRepository.findByEmailIgnoreCase("customer@noura.test")).thenReturn(Optional.of(user));
        when(notificationRepository.findByTargetUserAndReadFalse(user)).thenReturn(List.of());

        int updated = notificationService.markAllAsRead();

        assertEquals(0, updated);
        verify(notificationRepository, never()).saveAll(anyList());
    }

    @Test
    void markAsRead_shouldRejectWhenNotificationBelongsToAnotherUser() {
        UUID notificationId = UUID.randomUUID();
        UserAccount user = new UserAccount();
        user.setEmail("customer@noura.test");

        when(userAccountRepository.findByEmailIgnoreCase("customer@noura.test")).thenReturn(Optional.of(user));
        when(notificationRepository.findByIdAndTargetUser(notificationId, user)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> notificationService.markAsRead(notificationId));
        verify(notificationRepository, never()).save(org.mockito.ArgumentMatchers.any(Notification.class));
    }
}
