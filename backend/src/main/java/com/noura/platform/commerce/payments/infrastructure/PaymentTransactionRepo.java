package com.noura.platform.commerce.payments.infrastructure;

import com.noura.platform.commerce.payments.domain.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentTransactionRepo extends JpaRepository<PaymentTransaction, Long> {
    List<PaymentTransaction> findByOrder_IdOrderByCreatedAtDesc(Long orderId);

    Optional<PaymentTransaction> findByOrder_IdAndIdAndOrder_CustomerAccount_Id(Long orderId,
                                                                              Long paymentId,
                                                                              Long customerId);

    Optional<PaymentTransaction> findFirstByOrder_IdOrderByCreatedAtDesc(Long orderId);

    Optional<PaymentTransaction> findByProviderReference(String providerReference);
}
