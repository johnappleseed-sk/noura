package com.noura.platform.service.impl.recovery;

import com.noura.platform.dto.recovery.RecoveryBulkActionRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Executes bulk destructive-action jobs asynchronously.
 */
@Service
public class RecoveryBulkJobProcessor {

    private final RecoveryJobRunner recoveryJobRunner;

    /**
     * Creates a new recovery bulk-job processor.
     *
     * @param recoveryJobRunner The bulk job runner.
     */
    public RecoveryBulkJobProcessor(RecoveryJobRunner recoveryJobRunner) {
        this.recoveryJobRunner = recoveryJobRunner;
    }

    /**
     * Processes a queued bulk-action job asynchronously.
     *
     * @param jobId The job identifier.
     * @param request The bulk action payload.
     * @param actor The authenticated actor name.
     * @param tenantKey The tenant scope.
     */
    @Async
    public void processJob(UUID jobId, RecoveryBulkActionRequest request, String actor, String tenantKey) {
        recoveryJobRunner.processJob(jobId, request, actor, tenantKey);
    }
}
