package com.noura.customer.repository;

import com.noura.customer.domain.entity.CustomerProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for customer profile aggregates.
 */
public interface CustomerProfileRepository extends JpaRepository<CustomerProfile, UUID> {

    /**
     * Finds a customer profile by external identity subject.
     *
     * @param externalSubject external subject key
     * @return matching customer profile when found
     */
    Optional<CustomerProfile> findByExternalSubject(String externalSubject);
}
