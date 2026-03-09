package com.noura.platform.inventory.repository;

import com.noura.platform.inventory.domain.IamUserRole;
import com.noura.platform.inventory.domain.id.IamUserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IamUserRoleRepository extends JpaRepository<IamUserRole, IamUserRoleId> {

    @Query("""
            select userRole from IamUserRole userRole
            join fetch userRole.role role
            where userRole.user.id = :userId
            order by role.code asc
            """)
    List<IamUserRole> findDetailedByUserId(@Param("userId") String userId);
}
