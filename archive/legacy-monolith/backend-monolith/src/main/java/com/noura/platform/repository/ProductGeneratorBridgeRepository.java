package com.noura.platform.repository;

import com.noura.platform.domain.entity.ProductGeneratorBridge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProductGeneratorBridgeRepository extends JpaRepository<ProductGeneratorBridge, UUID> {

    Optional<ProductGeneratorBridge> findByCommerceProduct_Id(UUID commerceProductId);
}
