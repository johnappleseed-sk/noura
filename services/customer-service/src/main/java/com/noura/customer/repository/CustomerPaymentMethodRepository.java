package com.noura.customer.repository;

import com.noura.customer.domain.entity.CustomerPaymentMethod;
import com.noura.customer.domain.entity.CustomerProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for persisted customer payment methods.
 */
public interface CustomerPaymentMethodRepository extends JpaRepository<CustomerPaymentMethod, UUID> {

    List<CustomerPaymentMethod> findByCustomerOrderByUpdatedAtDesc(CustomerProfile customer);

    Optional<CustomerPaymentMethod> findByIdAndCustomer(UUID id, CustomerProfile customer);
}
