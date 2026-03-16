package com.company.platform.auth.repository;

import com.company.platform.auth.entity.UserRole;
import com.company.platform.auth.entity.id.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {

    boolean existsByIdUserIdAndIdRoleId(UUID userId, UUID roleId);
}
