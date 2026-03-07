package com.noura.platform.commerce.customers.infrastructure;

import com.noura.platform.commerce.customers.domain.CustomerAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerAccountRepo extends JpaRepository<CustomerAccount, Long> {
    boolean existsByEmailIgnoreCase(String email);

    Optional<CustomerAccount> findByEmailIgnoreCase(String email);
}
