package com.company.platform.auth.repository;

import com.company.platform.auth.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PermissionRepository extends JpaRepository<Permission, UUID> {

    Optional<Permission> findByScopeIgnoreCaseAndActionIgnoreCase(String scope, String action);
}
