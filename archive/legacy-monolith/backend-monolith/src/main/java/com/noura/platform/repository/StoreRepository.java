package com.noura.platform.repository;

import com.noura.platform.domain.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface StoreRepository extends JpaRepository<Store, UUID>, JpaSpecificationExecutor<Store> {
    boolean existsByStoreCodeIgnoreCase(String storeCode);

    boolean existsByStoreCodeIgnoreCaseAndIdNot(String storeCode, UUID id);

    boolean existsBySlugIgnoreCase(String slug);

    boolean existsBySlugIgnoreCaseAndIdNot(String slug, UUID id);
}
