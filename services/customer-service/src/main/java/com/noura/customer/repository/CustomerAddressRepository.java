package com.noura.customer.repository;

import com.noura.customer.domain.entity.CustomerAddress;
import com.noura.customer.domain.entity.CustomerProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for customer address entities.
 */
public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, UUID> {

    /**
     * Returns all addresses for a customer ordered by update timestamp descending.
     *
     * @param customer customer profile owner
     * @return ordered addresses
     */
    List<CustomerAddress> findByCustomerOrderByUpdatedAtDesc(CustomerProfile customer);

    /**
     * Finds one address by ID constrained to owner profile.
     *
     * @param id address ID
     * @param customer customer profile owner
     * @return matching address when found
     */
    Optional<CustomerAddress> findByIdAndCustomer(UUID id, CustomerProfile customer);
}
