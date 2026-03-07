package com.noura.platform.commerce.b2b.infrastructure;

import com.noura.platform.commerce.b2b.domain.Company;
import com.noura.platform.commerce.b2b.domain.CompanyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyRepo extends JpaRepository<Company, Long> {

    Optional<Company> findByTaxId(String taxId);

    List<Company> findByStatus(CompanyStatus status);

    List<Company> findByStatusOrderByName(CompanyStatus status);

    @Query("SELECT c FROM Company c WHERE c.status = :status")
    Page<Company> findByStatus(CompanyStatus status, Pageable pageable);

    @Query("SELECT c FROM Company c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :q, '%'))")
    Page<Company> searchByName(String q, Pageable pageable);

    List<Company> findBySalesRepId(Long salesRepId);

    @Query("SELECT c FROM Company c WHERE c.currentBalance > c.creditLimit")
    List<Company> findOverCreditLimit();
}
