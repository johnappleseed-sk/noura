package com.noura.platform.repository;

import com.noura.platform.domain.entity.CategoryChangeRequest;
import com.noura.platform.domain.enums.CategoryChangeRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CategoryChangeRequestRepository extends JpaRepository<CategoryChangeRequest, UUID> {
    /**
     * Finds all by order by created at desc.
     *
     * @return A list of matching items.
     */
    List<CategoryChangeRequest> findAllByOrderByCreatedAtDesc();

    /**
     * Finds by status order by created at desc.
     *
     * @param status The status value.
     * @return A list of matching items.
     */
    List<CategoryChangeRequest> findByStatusOrderByCreatedAtDesc(CategoryChangeRequestStatus status);
}
