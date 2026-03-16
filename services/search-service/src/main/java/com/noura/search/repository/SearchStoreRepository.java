package com.noura.search.repository;

import com.noura.search.domain.entity.SearchStore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SearchStoreRepository extends JpaRepository<SearchStore, UUID> {
    List<SearchStore> findTop10ByNameContainingIgnoreCaseOrderByNameAsc(String name);
}
