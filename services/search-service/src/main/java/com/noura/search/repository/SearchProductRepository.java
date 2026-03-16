package com.noura.search.repository;

import com.noura.search.domain.entity.SearchProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SearchProductRepository extends JpaRepository<SearchProduct, UUID> {
    List<SearchProduct> findTop20ByActiveTrueAndNameContainingIgnoreCaseOrderByUpdatedAtDesc(String name);

    List<SearchProduct> findTop20ByActiveTrueAndTrendingTrueOrderByPopularityScoreDesc();
}
