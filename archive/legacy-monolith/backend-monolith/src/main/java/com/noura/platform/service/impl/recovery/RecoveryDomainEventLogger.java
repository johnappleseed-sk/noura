package com.noura.platform.service.impl.recovery;

import com.noura.platform.event.RecoveryDomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Writes recovery domain events to the application log for operational observability.
 */
@Component
public class RecoveryDomainEventLogger {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecoveryDomainEventLogger.class);

    /**
     * Logs a published recovery domain event.
     *
     * @param event The published recovery domain event.
     */
    @EventListener
    public void onRecoveryEvent(RecoveryDomainEvent event) {
        LOGGER.info(
                "recovery-event tenant={} entityType={} entityId={} action={} state={} actor={} success={}",
                event.tenantKey(),
                event.entityType(),
                event.entityId(),
                event.actionType(),
                event.lifecycleState(),
                event.actor(),
                event.successful()
        );
    }
}
