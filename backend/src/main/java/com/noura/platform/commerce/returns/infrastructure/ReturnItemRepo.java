package com.noura.platform.commerce.returns.infrastructure;

import com.noura.platform.commerce.returns.domain.ReturnItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReturnItemRepo extends JpaRepository<ReturnItem, Long> {

    List<ReturnItem> findByReturnRequest_Id(Long returnRequestId);

    List<ReturnItem> findByOrderItem_Id(Long orderItemId);

    void deleteByReturnRequest_Id(Long returnRequestId);
}
