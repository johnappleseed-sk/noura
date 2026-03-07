package com.noura.platform.commerce.customers.infrastructure;

import com.noura.platform.commerce.customers.domain.CustomerAddress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerAddressRepo extends JpaRepository<CustomerAddress, Long> {
    List<CustomerAddress> findByCustomerAccount_IdOrderByIdAsc(Long customerAccountId);

    Optional<CustomerAddress> findByCustomerAccount_IdAndId(Long customerAccountId, Long id);

    Optional<CustomerAddress> findByIdAndCustomerAccount_Id(Long id, Long customerAccountId);

    boolean existsByIdAndCustomerAccount_Id(Long id, Long customerAccountId);

    Optional<CustomerAddress> findTopByCustomerAccount_IdAndDefaultShippingTrueOrderByIdAsc(Long customerAccountId);

    Optional<CustomerAddress> findFirstByCustomerAccount_IdOrderByIdAsc(Long customerAccountId);
}
