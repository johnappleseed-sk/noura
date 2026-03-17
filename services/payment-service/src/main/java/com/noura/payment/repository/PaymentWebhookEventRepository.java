package com.noura.payment.repository;

import com.noura.payment.domain.entity.PaymentWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for webhook delivery records.
 */
public interface PaymentWebhookEventRepository extends JpaRepository<PaymentWebhookEvent, UUID> {

    /**
     * Finds one delivery by provider identity and event identifier.
     *
     * @param providerCode provider code
     * @param providerEventId provider event identifier
     * @return optional webhook delivery
     */
    Optional<PaymentWebhookEvent> findByProviderCodeAndProviderEventId(String providerCode, String providerEventId);
}
