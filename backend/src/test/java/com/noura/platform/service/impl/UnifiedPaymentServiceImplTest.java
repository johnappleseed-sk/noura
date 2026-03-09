package com.noura.platform.service.impl;

import com.noura.platform.commerce.payments.application.StorefrontPaymentService;
import com.noura.platform.domain.enums.PaymentMethodType;
import com.noura.platform.dto.payment.CreatePaymentRequest;
import com.noura.platform.dto.payment.PaymentTransactionResult;
import com.noura.platform.dto.user.PaymentMethodDto;
import com.noura.platform.dto.user.PaymentMethodRequest;
import com.noura.platform.service.UserAccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnifiedPaymentServiceImplTest {

    @Mock
    private UserAccountService userAccountService;

    @Mock
    private StorefrontPaymentService storefrontPaymentService;

    private UnifiedPaymentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UnifiedPaymentServiceImpl(userAccountService, paymentProvider(null));
    }

    @Test
    void listPaymentMethodsDelegatesToUserAccountService() {
        List<PaymentMethodDto> expected = List.of(new PaymentMethodDto(
                UUID.randomUUID(), PaymentMethodType.CARD, "Stripe", "tok_123", true
        ));
        when(userAccountService.listPaymentMethods()).thenReturn(expected);

        List<PaymentMethodDto> actual = service.listPaymentMethods();

        assertThat(actual).isSameAs(expected);
        verify(userAccountService).listPaymentMethods();
    }

    @Test
    void addPaymentMethodDelegatesToUserAccountService() {
        var request = new PaymentMethodRequest(PaymentMethodType.CARD, "Stripe", "tok_456", true);
        var expected = new PaymentMethodDto(UUID.randomUUID(), PaymentMethodType.CARD, "Stripe", "tok_456", true);
        when(userAccountService.addPaymentMethod(request)).thenReturn(expected);

        PaymentMethodDto actual = service.addPaymentMethod(request);

        assertThat(actual).isSameAs(expected);
        verify(userAccountService).addPaymentMethod(request);
    }

    @Test
    void deletePaymentMethodDelegatesToUserAccountService() {
        UUID paymentMethodId = UUID.randomUUID();

        service.deletePaymentMethod(paymentMethodId);

        verify(userAccountService).deletePaymentMethod(paymentMethodId);
    }

    @Test
    void listStorefrontPaymentsFailsFastWhenLegacyServiceIsInactive() {
        assertThatThrownBy(() -> service.listStorefrontPayments(1L, 99L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Legacy storefront payment service is not active");
    }

    @Test
    void createStorefrontPaymentDelegatesWhenLegacyServiceIsAvailable() {
        var expected = new PaymentTransactionResult(
                10L, "Stripe", "CARD", "PENDING", BigDecimal.TEN, "USD",
                "pi_123", null, LocalDateTime.now(), LocalDateTime.now()
        );
        when(storefrontPaymentService.createInitialPayment(
                1L, 42L,
                new CreatePaymentRequest("CARD", "Stripe", "pi_123")
        )).thenReturn(expected);
        service = new UnifiedPaymentServiceImpl(userAccountService, paymentProvider(storefrontPaymentService));

        var actual = service.createStorefrontPayment(
                1L, 42L,
                new CreatePaymentRequest("CARD", "Stripe", "pi_123")
        );

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void captureStorefrontPaymentDelegatesWhenLegacyServiceIsAvailable() {
        var expected = new PaymentTransactionResult(
                10L, "Stripe", "CARD", "CAPTURED", BigDecimal.TEN, "USD",
                "pi_123", null, LocalDateTime.now(), LocalDateTime.now()
        );
        when(storefrontPaymentService.capture(1L, 42L, 10L)).thenReturn(expected);
        service = new UnifiedPaymentServiceImpl(userAccountService, paymentProvider(storefrontPaymentService));

        var actual = service.captureStorefrontPayment(1L, 42L, 10L);

        assertThat(actual).isEqualTo(expected);
    }

    private ObjectProvider<StorefrontPaymentService> paymentProvider(StorefrontPaymentService svc) {
        return new ObjectProvider<>() {
            @Override
            public StorefrontPaymentService getObject(Object... args) { return getIfAvailable(); }
            @Override
            public StorefrontPaymentService getIfAvailable() { return svc; }
            @Override
            public StorefrontPaymentService getIfUnique() { return svc; }
            @Override
            public StorefrontPaymentService getObject() {
                if (svc == null) throw new IllegalStateException("Legacy storefront payment service is not active in the current runtime profile.");
                return svc;
            }
            @Override
            public Iterator<StorefrontPaymentService> iterator() {
                return svc == null ? List.<StorefrontPaymentService>of().iterator() : List.of(svc).iterator();
            }
            @Override
            public Stream<StorefrontPaymentService> stream() {
                return svc == null ? Stream.empty() : Stream.of(svc);
            }
        };
    }
}
