package com.noura.platform.repository;

import com.noura.platform.domain.entity.UserAccount;
import com.noura.platform.domain.entity.UserLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserLocationRepository extends JpaRepository<UserLocation, UUID> {
    Optional<UserLocation> findFirstByUserOrderByCapturedAtDesc(UserAccount user);
}

