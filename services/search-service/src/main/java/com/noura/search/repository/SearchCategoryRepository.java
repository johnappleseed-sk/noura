package com.noura.search.repository;

import com.noura.search.domain.entity.SearchCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface SearchCategoryRepository extends JpaRepository<SearchCategory, UUID> {
    List<SearchCategory> findByIdIn(Collection<UUID> ids);
}
