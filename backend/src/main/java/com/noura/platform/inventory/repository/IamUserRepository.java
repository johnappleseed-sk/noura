package com.noura.platform.inventory.repository;

import com.noura.platform.inventory.domain.IamUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IamUserRepository extends JpaRepository<IamUser, String> {

    Optional<IamUser> findByIdAndDeletedAtIsNull(String userId);

    Optional<IamUser> findByUsernameIgnoreCaseAndDeletedAtIsNull(String username);

    Optional<IamUser> findByEmailIgnoreCaseAndDeletedAtIsNull(String email);

    boolean existsByUsernameIgnoreCaseAndDeletedAtIsNull(String username);

    boolean existsByEmailIgnoreCaseAndDeletedAtIsNull(String email);
}
