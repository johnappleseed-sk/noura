package com.noura.platform.service;

import com.noura.platform.config.AppProperties;
import com.noura.platform.mapper.NotificationMapper;
import com.noura.platform.repository.NotificationRepository;
import com.noura.platform.repository.UserAccountRepository;
import com.noura.platform.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

@ExtendWith(SpringExtension.class)
@ActiveProfiles("method-security-test")
@ContextConfiguration(classes = NotificationServiceMethodSecurityTest.Config.class)
class NotificationServiceMethodSecurityTest {

    @jakarta.annotation.Resource
    private NotificationService notificationService;

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void pushToUser_shouldDenyNonAdminRole() {
        assertThrows(AccessDeniedException.class, () -> notificationService.pushToUser(UUID.randomUUID(), null));
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class Config {

        @Bean NotificationRepository notificationRepository() { return mock(NotificationRepository.class); }
        @Bean UserAccountRepository userAccountRepository() { return mock(UserAccountRepository.class); }
        @Bean NotificationMapper notificationMapper() { return mock(NotificationMapper.class); }
        @Bean AppProperties appProperties() { return new AppProperties(); }

        @Bean
        NotificationService notificationService(
                NotificationRepository notificationRepository,
                UserAccountRepository userAccountRepository,
                NotificationMapper notificationMapper,
                AppProperties appProperties
        ) {
            return new NotificationServiceImpl(
                    notificationRepository,
                    userAccountRepository,
                    notificationMapper,
                    null,
                    appProperties,
                    null
            );
        }
    }
}
