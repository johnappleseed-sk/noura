package com.noura.platform.service;

import com.noura.platform.mapper.OrderMapper;
import com.noura.platform.repository.OrderItemRepository;
import com.noura.platform.repository.OrderRepository;
import com.noura.platform.repository.OrderTimelineEventRepository;
import com.noura.platform.repository.UserAccountRepository;
import com.noura.platform.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = OrderServiceMethodSecurityTest.Config.class)
class OrderServiceMethodSecurityTest {

    @jakarta.annotation.Resource
    private OrderService orderService;

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void adminOrders_shouldDenyNonAdminRole() {
        assertThrows(AccessDeniedException.class, () -> orderService.adminOrders(PageRequest.of(0, 5)));
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class Config {

        @Bean OrderRepository orderRepository() { return mock(OrderRepository.class); }
        @Bean OrderItemRepository orderItemRepository() { return mock(OrderItemRepository.class); }
        @Bean OrderTimelineEventRepository orderTimelineEventRepository() { return mock(OrderTimelineEventRepository.class); }
        @Bean OrderMapper orderMapper() { return mock(OrderMapper.class); }
        @Bean NotificationService notificationService() { return mock(NotificationService.class); }
        @Bean UserAccountRepository userAccountRepository() { return mock(UserAccountRepository.class); }

        @Bean
        OrderService orderService(
                OrderRepository orderRepository,
                OrderItemRepository orderItemRepository,
                OrderTimelineEventRepository orderTimelineEventRepository,
                OrderMapper orderMapper,
                NotificationService notificationService,
                UserAccountRepository userAccountRepository
        ) {
            return new OrderServiceImpl(
                    orderRepository,
                    orderItemRepository,
                    orderTimelineEventRepository,
                    orderMapper,
                    notificationService,
                    userAccountRepository
            );
        }
    }
}
