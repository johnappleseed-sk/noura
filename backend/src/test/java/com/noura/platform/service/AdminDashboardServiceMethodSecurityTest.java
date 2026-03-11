package com.noura.platform.service;

import com.noura.platform.mapper.ApprovalMapper;
import com.noura.platform.repository.ApprovalRequestRepository;
import com.noura.platform.repository.OrderRepository;
import com.noura.platform.repository.ProductRepository;
import com.noura.platform.repository.StoreRepository;
import com.noura.platform.repository.UserAccountRepository;
import com.noura.platform.service.impl.AdminDashboardServiceImpl;
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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

@ExtendWith(SpringExtension.class)
@ActiveProfiles("method-security-test")
@ContextConfiguration(classes = AdminDashboardServiceMethodSecurityTest.Config.class)
class AdminDashboardServiceMethodSecurityTest {

    @jakarta.annotation.Resource
    private AdminDashboardService adminDashboardService;

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void summary_shouldDenyNonAdminRole() {
        assertThrows(AccessDeniedException.class, () -> adminDashboardService.summary());
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class Config {

        @Bean OrderRepository orderRepository() { return mock(OrderRepository.class); }
        @Bean ProductRepository productRepository() { return mock(ProductRepository.class); }
        @Bean StoreRepository storeRepository() { return mock(StoreRepository.class); }
        @Bean UserAccountRepository userAccountRepository() { return mock(UserAccountRepository.class); }
        @Bean ApprovalRequestRepository approvalRequestRepository() { return mock(ApprovalRequestRepository.class); }
        @Bean ApprovalMapper approvalMapper() { return mock(ApprovalMapper.class); }

        @Bean
        AdminDashboardService adminDashboardService(
                OrderRepository orderRepository,
                ProductRepository productRepository,
                StoreRepository storeRepository,
                UserAccountRepository userAccountRepository,
                ApprovalRequestRepository approvalRequestRepository,
                ApprovalMapper approvalMapper
        ) {
            return new AdminDashboardServiceImpl(
                    orderRepository,
                    productRepository,
                    storeRepository,
                    userAccountRepository,
                    approvalRequestRepository,
                    approvalMapper
            );
        }
    }
}
