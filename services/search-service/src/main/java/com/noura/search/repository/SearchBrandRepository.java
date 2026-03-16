package com.noura.search.repository;

import com.noura.search.domain.entity.SearchBrand;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SearchBrandRepository extends JpaRepository<SearchBrand, UUID> {
    List<SearchBrand> findTop10ByActiveTrueAndNameContainingIgnoreCaseOrderByNameAsc(String name);
}
