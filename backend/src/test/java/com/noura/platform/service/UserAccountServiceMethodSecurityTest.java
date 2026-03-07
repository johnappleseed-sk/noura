package com.noura.platform.service;

import com.noura.platform.mapper.*;
import com.noura.platform.repository.*;
import com.noura.platform.service.impl.UserAccountServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = UserAccountServiceMethodSecurityTest.Config.class)
class UserAccountServiceMethodSecurityTest {

    @jakarta.annotation.Resource
    private UserAccountService userAccountService;

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void listUsers_shouldDenyNonAdminRole() {
        assertThrows(AccessDeniedException.class, () -> userAccountService.listUsers(PageRequest.of(0, 10)));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void upsertCompanyProfile_shouldDenyNonB2BRole() {
        assertThrows(AccessDeniedException.class, () -> userAccountService.upsertCompanyProfile(null));
    }

    @Configuration
    @EnableMethodSecurity
    static class Config {

        @Bean UserAccountRepository userAccountRepository() { return mock(UserAccountRepository.class); }
        @Bean AddressRepository addressRepository() { return mock(AddressRepository.class); }
        @Bean PaymentMethodRepository paymentMethodRepository() { return mock(PaymentMethodRepository.class); }
        @Bean OrderRepository orderRepository() { return mock(OrderRepository.class); }
        @Bean OrderItemRepository orderItemRepository() { return mock(OrderItemRepository.class); }
        @Bean CartRepository cartRepository() { return mock(CartRepository.class); }
        @Bean CartItemRepository cartItemRepository() { return mock(CartItemRepository.class); }
        @Bean B2BCompanyProfileRepository companyProfileRepository() { return mock(B2BCompanyProfileRepository.class); }
        @Bean ApprovalRequestRepository approvalRequestRepository() { return mock(ApprovalRequestRepository.class); }
        @Bean UserMapper userMapper() { return mock(UserMapper.class); }
        @Bean AddressMapper addressMapper() { return mock(AddressMapper.class); }
        @Bean PaymentMethodMapper paymentMethodMapper() { return mock(PaymentMethodMapper.class); }
        @Bean CompanyMapper companyMapper() { return mock(CompanyMapper.class); }
        @Bean ApprovalMapper approvalMapper() { return mock(ApprovalMapper.class); }
        @Bean OrderMapper orderMapper() { return mock(OrderMapper.class); }

        @Bean
        UserAccountService userAccountService(
                UserAccountRepository userAccountRepository,
                AddressRepository addressRepository,
                PaymentMethodRepository paymentMethodRepository,
                OrderRepository orderRepository,
                OrderItemRepository orderItemRepository,
                CartRepository cartRepository,
                CartItemRepository cartItemRepository,
                B2BCompanyProfileRepository companyProfileRepository,
                ApprovalRequestRepository approvalRequestRepository,
                UserMapper userMapper,
                AddressMapper addressMapper,
                PaymentMethodMapper paymentMethodMapper,
                CompanyMapper companyMapper,
                ApprovalMapper approvalMapper,
                OrderMapper orderMapper
        ) {
            return new UserAccountServiceImpl(
                    userAccountRepository,
                    addressRepository,
                    paymentMethodRepository,
                    orderRepository,
                    orderItemRepository,
                    cartRepository,
                    cartItemRepository,
                    companyProfileRepository,
                    approvalRequestRepository,
                    userMapper,
                    addressMapper,
                    paymentMethodMapper,
                    companyMapper,
                    approvalMapper,
                    orderMapper
            );
        }
    }
}
