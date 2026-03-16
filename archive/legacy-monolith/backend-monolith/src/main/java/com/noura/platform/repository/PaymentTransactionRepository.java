package com.noura.platform.repository;

import com.noura.platform.domain.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {
    Optional<PaymentTransaction> findByIdAndOrder_User_EmailIgnoreCase(UUID id, String email);

    Optional<PaymentTransaction> findByPaymentReferenceAndProviderCodeIgnoreCase(String paymentReference, String providerCode);
}
