package com.noura.platform.inventory.repository;

import com.noura.platform.inventory.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface InventoryCategoryRepository extends JpaRepository<Category, String>, JpaSpecificationExecutor<Category> {

    Optional<Category> findByIdAndDeletedAtIsNull(String id);

    boolean existsByCategoryCodeIgnoreCaseAndDeletedAtIsNull(String categoryCode);

    boolean existsByCategoryCodeIgnoreCaseAndDeletedAtIsNullAndIdNot(String categoryCode, String id);

    boolean existsByParent_IdAndDeletedAtIsNull(String parentId);

    List<Category> findAllByDeletedAtIsNullOrderByLevelAscSortOrderAscNameAsc();

    List<Category> findAllByParent_IdAndDeletedAtIsNullOrderBySortOrderAscNameAsc(String parentId);
}
