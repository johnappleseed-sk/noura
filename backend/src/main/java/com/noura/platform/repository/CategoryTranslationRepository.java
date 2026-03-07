package com.noura.platform.repository;

import com.noura.platform.domain.entity.CategoryTranslation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryTranslationRepository extends JpaRepository<CategoryTranslation, UUID> {
    /**
     * Finds by category id order by locale asc.
     *
     * @param categoryId The category id used to locate the target record.
     * @return A list of matching items.
     */
    List<CategoryTranslation> findByCategoryIdOrderByLocaleAsc(UUID categoryId);

    /**
     * Finds by locale ignore case.
     *
     * @param locale The locale value.
     * @return A list of matching items.
     */
    List<CategoryTranslation> findByLocaleIgnoreCase(String locale);

    /**
     * Finds by category id and locale ignore case.
     *
     * @param categoryId The category id used to locate the target record.
     * @param locale The locale value.
     * @return The matching translation when available.
     */
    Optional<CategoryTranslation> findByCategoryIdAndLocaleIgnoreCase(UUID categoryId, String locale);
}
