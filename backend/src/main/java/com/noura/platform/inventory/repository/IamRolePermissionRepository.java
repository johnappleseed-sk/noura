package com.noura.platform.inventory.repository;

import com.noura.platform.inventory.domain.IamRolePermission;
import com.noura.platform.inventory.domain.id.IamRolePermissionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface IamRolePermissionRepository extends JpaRepository<IamRolePermission, IamRolePermissionId> {

    @Query("""
            select rolePermission from IamRolePermission rolePermission
            join fetch rolePermission.permission permission
            where rolePermission.role.id in :roleIds
            order by permission.code asc
            """)
    List<IamRolePermission> findDetailedByRoleIds(@Param("roleIds") Collection<String> roleIds);
}
