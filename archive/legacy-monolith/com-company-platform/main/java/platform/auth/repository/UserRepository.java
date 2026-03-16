package com.company.platform.auth.repository;

import com.company.platform.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByUsernameIgnoreCase(String username);

    @Query("""
            select distinct u
            from User u
            left join fetch u.userRoles ur
            left join fetch ur.role r
            left join fetch r.rolePermissions rp
            left join fetch rp.permission
            where lower(u.email) = lower(:email)
            """)
    Optional<User> findDetailedByEmail(String email);

    @Query("""
            select distinct u
            from User u
            left join fetch u.userRoles ur
            left join fetch ur.role r
            left join fetch r.rolePermissions rp
            left join fetch rp.permission
            where u.id = :id
            """)
    Optional<User> findDetailedById(UUID id);

    @Query("""
            select distinct u
            from User u
            left join fetch u.userRoles ur
            left join fetch ur.role r
            left join fetch r.rolePermissions rp
            left join fetch rp.permission
            order by u.createdAt desc
            """)
    List<User> findAllDetailed();
}
