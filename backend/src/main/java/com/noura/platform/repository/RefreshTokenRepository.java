package com.noura.platform.repository;

import com.noura.platform.domain.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    /**
     * Finds by token and revoked false.
     *
     * @param token The token value.
     * @return The result of find by token and revoked false.
     */
    Optional<RefreshToken> findByTokenAndRevokedFalse(String token);

    /**
     * Deletes by expires at before.
     *
     * @param instant The instant value.
     */
    void deleteByExpiresAtBefore(Instant instant);
}
