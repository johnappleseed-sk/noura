package com.noura.platform.commerce.b2b.infrastructure;

import com.noura.platform.commerce.b2b.domain.POStatus;
import com.noura.platform.commerce.b2b.domain.PurchaseOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseOrderRepo extends JpaRepository<PurchaseOrder, Long> {

    Optional<PurchaseOrder> findByPoNumber(String poNumber);

    List<PurchaseOrder> findByCompanyIdOrderByCreatedAtDesc(Long companyId);

    Page<PurchaseOrder> findByCompanyId(Long companyId, Pageable pageable);

    List<PurchaseOrder> findByStatus(POStatus status);

    Page<PurchaseOrder> findByStatus(POStatus status, Pageable pageable);

    @Query("SELECT po FROM PurchaseOrder po WHERE po.company.id = :companyId AND po.status = :status")
    List<PurchaseOrder> findByCompanyIdAndStatus(Long companyId, POStatus status);

    @Query("SELECT po FROM PurchaseOrder po WHERE po.company.id = :companyId AND po.status = :status")
    Page<PurchaseOrder> findByCompanyIdAndStatus(Long companyId, POStatus status, Pageable pageable);

    @Query("SELECT po FROM PurchaseOrder po WHERE po.paymentDueDate < CURRENT_DATE " +
            "AND po.amountPaid < po.totalAmount AND po.status NOT IN ('CANCELLED', 'DRAFT')")
    List<PurchaseOrder> findOverdueOrders();
}
