package com.noura.platform.inventory.repository;

import com.noura.platform.inventory.domain.IamPermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface IamPermissionRepository extends JpaRepository<IamPermission, String> {

    List<IamPermission> findAllByCodeIn(Collection<String> codes);
}
