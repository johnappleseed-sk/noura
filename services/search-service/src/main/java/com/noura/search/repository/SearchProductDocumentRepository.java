package com.noura.search.repository;

import com.noura.search.domain.entity.SearchProductDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Repository for search-owned denormalized product documents.
 */
public interface SearchProductDocumentRepository extends JpaRepository<SearchProductDocument, UUID> {

    /**
     * Executes full-text and trigram-backed product search against active indexed documents.
     *
     * @param query free-text query
     * @param categoryId optional category filter
     * @param brandId optional brand filter
     * @param pageable page request
     * @return paged document hits
     */
    @Query(
            value = """
                    SELECT *
                    FROM search_product_documents d
                    WHERE d.active = TRUE
                      AND (
                           d.search_document @@ websearch_to_tsquery('simple', :query)
                           OR LOWER(d.name) LIKE LOWER(CONCAT('%', :query, '%'))
                           OR LOWER(COALESCE(d.product_code, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                           OR LOWER(COALESCE(d.brand_name, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                           OR LOWER(COALESCE(d.category_name, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                           OR LOWER(COALESCE(d.slug, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                           OR LOWER(COALESCE(d.short_description, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                      )
                      AND (:categoryId IS NULL OR d.category_id = :categoryId)
                      AND (:brandId IS NULL OR d.brand_id = :brandId)
                    ORDER BY d.trending DESC,
                             d.popularity_score DESC,
                             d.source_updated_at DESC NULLS LAST,
                             d.indexed_at DESC
                    """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM search_product_documents d
                    WHERE d.active = TRUE
                      AND (
                           d.search_document @@ websearch_to_tsquery('simple', :query)
                           OR LOWER(d.name) LIKE LOWER(CONCAT('%', :query, '%'))
                           OR LOWER(COALESCE(d.product_code, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                           OR LOWER(COALESCE(d.brand_name, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                           OR LOWER(COALESCE(d.category_name, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                           OR LOWER(COALESCE(d.slug, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                           OR LOWER(COALESCE(d.short_description, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                      )
                      AND (:categoryId IS NULL OR d.category_id = :categoryId)
                      AND (:brandId IS NULL OR d.brand_id = :brandId)
                    """,
            nativeQuery = true
    )
    Page<SearchProductDocument> searchActiveDocuments(
            @Param("query") String query,
            @Param("categoryId") UUID categoryId,
            @Param("brandId") UUID brandId,
            Pageable pageable
    );

    /**
     * Returns product-name suggestions.
     *
     * @param name source query
     * @return matching documents
     */
    List<SearchProductDocument> findTop20ByActiveTrueAndNameContainingIgnoreCaseOrderByPopularityScoreDescSourceUpdatedAtDesc(
            String name
    );

    /**
     * Returns brand-name suggestion candidates.
     *
     * @param brandName source query
     * @return matching documents
     */
    List<SearchProductDocument> findTop20ByActiveTrueAndBrandNameContainingIgnoreCaseOrderByBrandNameAscPopularityScoreDesc(
            String brandName
    );

    /**
     * Returns category-name suggestion candidates.
     *
     * @param categoryName source query
     * @return matching documents
     */
    List<SearchProductDocument> findTop20ByActiveTrueAndCategoryNameContainingIgnoreCaseOrderByCategoryNameAscPopularityScoreDesc(
            String categoryName
    );

    /**
     * Returns trending documents for trend-tag derivation.
     *
     * @return trending documents
     */
    List<SearchProductDocument> findTop20ByActiveTrueAndTrendingTrueOrderByPopularityScoreDescSourceUpdatedAtDesc();
}
