package com.noura.platform.repository;

import com.noura.platform.domain.entity.StoreProductReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface StoreProductReferenceRepository extends JpaRepository<StoreProductReference, UUID>, JpaSpecificationExecutor<StoreProductReference> {
    Optional<StoreProductReference> findByIdAndActiveTrue(UUID id);

    Optional<StoreProductReference> findByStoreIdAndProductId(UUID storeId, UUID productId);

    Page<StoreProductReference> findByStoreId(UUID storeId, Pageable pageable);

    Page<StoreProductReference> findByStoreIdAndProductId(UUID storeId, UUID productId, Pageable pageable);

    Page<StoreProductReference> findByStoreIdAndActive(UUID storeId, boolean active, Pageable pageable);

    Page<StoreProductReference> findByStoreIdAndProductIdAndActive(UUID storeId, UUID productId, boolean active, Pageable pageable);
}
