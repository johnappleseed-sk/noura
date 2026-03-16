package com.noura.platform.commerce.returns.infrastructure;

import com.noura.platform.commerce.returns.domain.ReturnRequest;
import com.noura.platform.commerce.returns.domain.ReturnStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReturnRequestRepo extends JpaRepository<ReturnRequest, Long> {

    Optional<ReturnRequest> findByReturnNumber(String returnNumber);

    Optional<ReturnRequest> findByIdAndCustomerAccount_Id(Long id, Long customerId);

    List<ReturnRequest> findByCustomerAccount_IdOrderByCreatedAtDesc(Long customerId);

    List<ReturnRequest> findByOrder_IdOrderByCreatedAtDesc(Long orderId);

    Page<ReturnRequest> findByStatus(ReturnStatus status, Pageable pageable);

    Page<ReturnRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByStatus(ReturnStatus status);

    boolean existsByReturnNumber(String returnNumber);
}
