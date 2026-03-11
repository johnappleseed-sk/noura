package com.noura.platform.service;

import com.noura.platform.common.exception.NotFoundException;
import com.noura.platform.domain.entity.Order;
import com.noura.platform.domain.entity.UserAccount;
import com.noura.platform.mapper.AddressMapper;
import com.noura.platform.mapper.ApprovalMapper;
import com.noura.platform.mapper.CompanyMapper;
import com.noura.platform.mapper.OrderMapper;
import com.noura.platform.mapper.PaymentMethodMapper;
import com.noura.platform.mapper.UserMapper;
import com.noura.platform.repository.AddressRepository;
import com.noura.platform.repository.ApprovalRequestRepository;
import com.noura.platform.repository.B2BCompanyProfileRepository;
import com.noura.platform.repository.CartItemRepository;
import com.noura.platform.repository.CartRepository;
import com.noura.platform.repository.OrderItemRepository;
import com.noura.platform.repository.OrderRepository;
import com.noura.platform.repository.PaymentMethodRepository;
import com.noura.platform.repository.UserAccountRepository;
import com.noura.platform.service.LocationIntelligenceService;
import com.noura.platform.service.impl.UserAccountServiceImpl;
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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAccountServiceOrderOwnershipTest {

    @Mock private UserAccountRepository userAccountRepository;
    @Mock private AddressRepository addressRepository;
    @Mock private PaymentMethodRepository paymentMethodRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private CartRepository cartRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private B2BCompanyProfileRepository companyProfileRepository;
    @Mock private ApprovalRequestRepository approvalRequestRepository;
    @Mock private UserMapper userMapper;
    @Mock private AddressMapper addressMapper;
    @Mock private PaymentMethodMapper paymentMethodMapper;
    @Mock private CompanyMapper companyMapper;
    @Mock private ApprovalMapper approvalMapper;
    @Mock private OrderMapper orderMapper;
    @Mock private LocationIntelligenceService locationIntelligenceService;

    private UserAccountServiceImpl userAccountService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "owner-a@noura.test",
                        "n/a",
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                )
        );
        userAccountService = new UserAccountServiceImpl(
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
                orderMapper,
                locationIntelligenceService
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void quickReorder_shouldRejectWhenOrderBelongsToAnotherUser() {
        UUID orderId = UUID.randomUUID();

        UserAccount ownerA = new UserAccount();
        ownerA.setId(UUID.randomUUID());
        ownerA.setEmail("owner-a@noura.test");

        UserAccount ownerB = new UserAccount();
        ownerB.setId(UUID.randomUUID());
        ownerB.setEmail("owner-b@noura.test");

        Order foreignOrder = new Order();
        foreignOrder.setId(orderId);
        foreignOrder.setUser(ownerB);

        when(userAccountRepository.findByEmailIgnoreCase("owner-a@noura.test")).thenReturn(Optional.of(ownerA));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(foreignOrder));

        assertThrows(NotFoundException.class, () -> userAccountService.quickReorder(orderId));
        verify(cartRepository, never()).findByUser(ownerA);
    }
}
