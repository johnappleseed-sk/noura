package com.noura.platform.repository;

import com.noura.platform.domain.entity.UserStoreAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserStoreAssignmentRepository extends JpaRepository<UserStoreAssignment, UUID> {
    Optional<UserStoreAssignment> findByUserIdAndStoreId(UUID userId, UUID storeId);

    Optional<UserStoreAssignment> findByUserIdAndStoreIdAndActiveTrue(UUID userId, UUID storeId);

    List<UserStoreAssignment> findByStoreIdAndActiveTrue(UUID storeId);

    List<UserStoreAssignment> findByUserIdAndActiveTrue(UUID userId);
}
