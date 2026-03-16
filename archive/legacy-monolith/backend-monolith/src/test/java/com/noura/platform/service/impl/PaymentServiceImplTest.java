package com.noura.platform.service.impl;

import com.noura.platform.domain.entity.Order;
import com.noura.platform.domain.entity.PaymentTransaction;
import com.noura.platform.domain.entity.UserAccount;
import com.noura.platform.domain.enums.OrderStatus;
import com.noura.platform.domain.enums.PaymentMethodType;
import com.noura.platform.domain.enums.PaymentStatus;
import com.noura.platform.dto.payment.CreatePaymentRequest;
import com.noura.platform.dto.payment.MockPaymentWebhookRequest;
import com.noura.platform.dto.payment.PaymentResponse;
import com.noura.platform.repository.OrderRepository;
import com.noura.platform.repository.PaymentTransactionRepository;
import com.noura.platform.repository.UserAccountRepository;
import com.noura.platform.service.payment.MockPaymentGateway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createPayment_shouldCreatePendingTransactionForOwnedOrder() {
        PaymentTransactionRepository paymentTransactionRepository = mock(PaymentTransactionRepository.class);
        OrderRepository orderRepository = mock(OrderRepository.class);
        UserAccountRepository userAccountRepository = mock(UserAccountRepository.class);

        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        UserAccount user = new UserAccount();
        user.setId(userId);
        user.setEmail("buyer@example.com");

        Order order = new Order();
        order.setId(orderId);
        order.setUser(user);
        order.setTotalAmount(new BigDecimal("149.99"));
        order.setStatus(OrderStatus.CREATED);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("buyer@example.com", "n/a", List.of())
        );

        when(userAccountRepository.findByEmailIgnoreCase("buyer@example.com")).thenReturn(Optional.of(user));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(invocation -> {
            PaymentTransaction transaction = invocation.getArgument(0);
            transaction.setId(UUID.randomUUID());
            transaction.setCreatedAt(Instant.now());
            transaction.setUpdatedAt(Instant.now());
            return transaction;
        });

        PaymentServiceImpl service = new PaymentServiceImpl(
                paymentTransactionRepository,
                orderRepository,
                userAccountRepository,
                List.of(new MockPaymentGateway())
        );

        PaymentResponse response = service.createPayment(
                new CreatePaymentRequest(orderId, PaymentMethodType.CARD, "mock", "usd", null, null, null)
        );

        ArgumentCaptor<PaymentTransaction> transactionCaptor = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(paymentTransactionRepository).save(transactionCaptor.capture());

        PaymentTransaction saved = transactionCaptor.getValue();
        assertThat(saved.getPaymentReference()).startsWith("PAY-");
        assertThat(saved.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(saved.getProviderCode()).isEqualTo("mock");
        assertThat(saved.getProviderTransactionId()).startsWith("mock_txn_");
        assertThat(response.status()).isEqualTo(PaymentStatus.PENDING);
        assertThat(response.amount()).isEqualByComparingTo("149.99");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);
        assertThat(order.getPaymentReference()).isEqualTo(response.paymentReference());
    }

    @Test
    void processMockWebhook_shouldMarkTransactionSuccessfulAndOrderPaid() {
        PaymentTransactionRepository paymentTransactionRepository = mock(PaymentTransactionRepository.class);
        OrderRepository orderRepository = mock(OrderRepository.class);
        UserAccountRepository userAccountRepository = mock(UserAccountRepository.class);

        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setStatus(OrderStatus.PAYMENT_PENDING);

        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setId(UUID.randomUUID());
        transaction.setOrder(order);
        transaction.setPaymentReference("PAY-ABC123");
        transaction.setMethodType(PaymentMethodType.CARD);
        transaction.setStatus(PaymentStatus.PENDING);
        transaction.setProviderCode("mock");
        transaction.setAmount(new BigDecimal("99.50"));
        transaction.setCurrencyCode("USD");
        transaction.setRequestedAt(Instant.now());
        transaction.setCreatedAt(Instant.now());
        transaction.setUpdatedAt(Instant.now());

        when(paymentTransactionRepository.findByPaymentReferenceAndProviderCodeIgnoreCase("PAY-ABC123", "mock"))
                .thenReturn(Optional.of(transaction));
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(invocation -> {
            PaymentTransaction saved = invocation.getArgument(0);
            saved.setUpdatedAt(Instant.now());
            return saved;
        });
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentServiceImpl service = new PaymentServiceImpl(
                paymentTransactionRepository,
                orderRepository,
                userAccountRepository,
                List.of(new MockPaymentGateway())
        );

        PaymentResponse response = service.processMockWebhook(
                new MockPaymentWebhookRequest("PAY-ABC123", PaymentStatus.SUCCESS, "mock_txn_final", null)
        );

        assertThat(response.status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(response.providerTransactionId()).isEqualTo("mock_txn_final");
        assertThat(response.completedAt()).isNotNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
    }
}
