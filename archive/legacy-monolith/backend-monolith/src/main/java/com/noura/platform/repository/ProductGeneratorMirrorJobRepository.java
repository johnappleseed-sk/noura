package com.noura.platform.repository;

import com.noura.platform.domain.entity.ProductGeneratorMirrorJob;
import com.noura.platform.domain.enums.ProductGeneratorMirrorJobStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductGeneratorMirrorJobRepository extends JpaRepository<ProductGeneratorMirrorJob, UUID> {

    Optional<ProductGeneratorMirrorJob> findFirstByCommerceProduct_IdOrderByCreatedAtDesc(UUID commerceProductId);

    @Query("""
            select job
            from ProductGeneratorMirrorJob job
            where job.status in :statuses
              and (job.nextRetryAt is null or job.nextRetryAt <= :now)
            order by job.createdAt asc
            """)
    List<ProductGeneratorMirrorJob> findDueJobs(
            @Param("statuses") Collection<ProductGeneratorMirrorJobStatus> statuses,
            @Param("now") Instant now,
            Pageable pageable
    );
}
