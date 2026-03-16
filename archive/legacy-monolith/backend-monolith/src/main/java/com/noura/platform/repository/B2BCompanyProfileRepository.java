package com.noura.platform.repository;

import com.noura.platform.domain.entity.B2BCompanyProfile;
import com.noura.platform.domain.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface B2BCompanyProfileRepository extends JpaRepository<B2BCompanyProfile, UUID> {
    /**
     * Finds by user.
     *
     * @param user The user context for this operation.
     * @return The result of find by user.
     */
    Optional<B2BCompanyProfile> findByUser(UserAccount user);
}
