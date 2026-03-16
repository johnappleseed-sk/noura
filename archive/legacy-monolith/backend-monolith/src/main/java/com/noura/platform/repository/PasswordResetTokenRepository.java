package com.noura.platform.repository;

import com.noura.platform.domain.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {
    /**
     * Finds by token hash and used false.
     *
     * @param tokenHash The token hash value.
     * @return The result of find by token hash and used false.
     */
    Optional<PasswordResetToken> findByTokenHashAndUsedFalse(String tokenHash);
}
