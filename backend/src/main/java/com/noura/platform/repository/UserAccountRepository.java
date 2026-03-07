package com.noura.platform.repository;

import com.noura.platform.domain.entity.UserAccount;
import com.noura.platform.domain.enums.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {
    /**
     * Finds by email ignore case.
     *
     * @param email The email value.
     * @return The result of find by email ignore case.
     */
    Optional<UserAccount> findByEmailIgnoreCase(String email);

    /**
     * Finds by role.
     *
     * @param role The role value.
     * @return A list of matching items.
     */
    @Query("select distinct u from UserAccount u join u.roles r where r = :role")
    List<UserAccount> findByRole(RoleType role);
}
