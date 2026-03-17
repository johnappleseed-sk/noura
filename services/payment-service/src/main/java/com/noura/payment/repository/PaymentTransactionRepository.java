package com.noura.payment.repository;

import com.noura.payment.domain.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for payment transaction aggregate records.
 */
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {

    /**
     * Finds one transaction by identifier and customer reference.
     *
     * @param id transaction identifier
     * @param customerRef customer reference
     * @return optional matching transaction
     */
    Optional<PaymentTransaction> findByIdAndCustomerRef(UUID id, String customerRef);

    /**
     * Finds one transaction by payment reference.
     *
     * @param paymentReference payment reference
     * @return optional matching transaction
     */
    Optional<PaymentTransaction> findByPaymentReference(String paymentReference);

    /**
     * Finds one transaction by provider code and provider transaction ID.
     *
     * @param providerCode provider code
     * @param providerTransactionId provider transaction ID
     * @return optional matching transaction
     */
    Optional<PaymentTransaction> findByProviderCodeAndProviderTransactionId(String providerCode, String providerTransactionId);

    /**
     * Finds one transaction by order, customer, and idempotency key.
     *
     * @param orderId order identifier
     * @param customerRef customer reference
     * @param idempotencyKey idempotency key
     * @return optional matching transaction
     */
    Optional<PaymentTransaction> findByOrderIdAndCustomerRefAndIdempotencyKey(UUID orderId, String customerRef, String idempotencyKey);

    /**
     * Finds one transaction by order ID and customer reference sorted by latest update.
     *
     * @param orderId order identifier
     * @param customerRef customer reference
     * @return optional latest matching transaction
     */
    Optional<PaymentTransaction> findFirstByOrderIdAndCustomerRefOrderByUpdatedAtDesc(UUID orderId, String customerRef);

    /**
     * Finds all transactions for one order sorted by latest update.
     *
     * @param orderId order identifier
     * @return transaction list
     */
    List<PaymentTransaction> findByOrderIdOrderByUpdatedAtDesc(UUID orderId);

    /**
     * Finds transaction by ID with a pessimistic write lock.
     *
     * @param id transaction identifier
     * @return optional locked transaction
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select t
            from PaymentTransaction t
            where t.id = :id
            """)
    Optional<PaymentTransaction> findByIdForUpdate(@Param("id") UUID id);

    /**
     * Finds one transaction by payment reference using a write lock.
     *
     * @param paymentReference payment reference
     * @return optional locked transaction
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select t
            from PaymentTransaction t
            where t.paymentReference = :paymentReference
            """)
    Optional<PaymentTransaction> findByPaymentReferenceForUpdate(@Param("paymentReference") String paymentReference);

    /**
     * Finds one transaction by provider identity using a write lock.
     *
     * @param providerCode provider code
     * @param providerTransactionId provider transaction identifier
     * @return optional locked transaction
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select t
            from PaymentTransaction t
            where t.providerCode = :providerCode
              and t.providerTransactionId = :providerTransactionId
            """)
    Optional<PaymentTransaction> findByProviderCodeAndProviderTransactionIdForUpdate(
            @Param("providerCode") String providerCode,
            @Param("providerTransactionId") String providerTransactionId
    );
}
