package com.noura.platform.service.impl.recovery;

import com.noura.platform.domain.entity.RecoveryRecord;
import com.noura.platform.domain.enums.RecoveryActionType;
import com.noura.platform.domain.enums.RecoveryLifecycleState;
import com.noura.platform.dto.recovery.RecoveryActionRequest;
import com.noura.platform.repository.RecoveryRecordRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Background worker responsible for retention-driven purge orchestration.
 */
@Component
public class RecoveryRetentionWorker {

    private final RecoveryRecordRepository recoveryRecordRepository;
    private final RecoveryGovernanceServiceImpl recoveryGovernanceService;

    /**
     * Creates a new recovery retention worker.
     *
     * @param recoveryRecordRepository The recovery record repository.
     * @param recoveryGovernanceService The recovery governance service implementation.
     */
    public RecoveryRetentionWorker(
            RecoveryRecordRepository recoveryRecordRepository,
            RecoveryGovernanceServiceImpl recoveryGovernanceService
    ) {
        this.recoveryRecordRepository = recoveryRecordRepository;
        this.recoveryGovernanceService = recoveryGovernanceService;
    }

    /**
     * Purges expired trash records that are no longer under legal hold.
     */
    @Scheduled(cron = "${app.recovery.retention-purge-cron:0 0 2 * * *}")
    public void purgeExpiredTrash() {
        Instant now = Instant.now();
        List<RecoveryRecord> expiredTrash = recoveryRecordRepository.findAll(expiredTrashSpec(now));
        for (RecoveryRecord record : expiredTrash) {
            recoveryGovernanceService.applyActionInternal(
                    new RecoveryActionRequest(
                            record.getEntityType(),
                            record.getEntityId(),
                            RecoveryActionType.HARD_DELETE,
                            "Retention window expired.",
                            null,
                            null,
                            null,
                            Map.of("source", "retention-worker")
                    ),
                    "system:retention-worker",
                    record.getTenantKey()
            );
        }
    }

    /**
     * Builds the query used to select expired trash records eligible for purge.
     *
     * @param now The current timestamp.
     * @return The JPA specification.
     */
    private Specification<RecoveryRecord> expiredTrashSpec(Instant now) {
        return (root, ignored, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("lifecycleState"), RecoveryLifecycleState.TRASHED));
            predicates.add(builder.isNotNull(root.get("retentionUntil")));
            predicates.add(builder.lessThan(root.get("retentionUntil"), now));
            predicates.add(builder.or(
                    builder.isNull(root.get("legalHoldUntil")),
                    builder.lessThan(root.get("legalHoldUntil"), now)
            ));
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
