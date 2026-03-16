package com.noura.platform.repository;

import com.noura.platform.domain.entity.CarouselSlide;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface CarouselSlideRepository extends JpaRepository<CarouselSlide, UUID>, JpaSpecificationExecutor<CarouselSlide> {
    boolean existsBySlugIgnoreCase(String slug);

    boolean existsBySlugIgnoreCaseAndIdNot(String slug, UUID id);

    Optional<CarouselSlide> findByPreviewToken(String previewToken);

    @Query("select coalesce(max(c.position), 0) from CarouselSlide c where c.deletedAt is null")
    Integer findMaxPosition();
}
