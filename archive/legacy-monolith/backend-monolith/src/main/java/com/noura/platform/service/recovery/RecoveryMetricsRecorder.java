package com.noura.platform.service.recovery;

import com.noura.platform.domain.enums.RecoveryActionType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * Emits Micrometer counters for governed destructive and recovery operations.
 */
@Service
public class RecoveryMetricsRecorder {

    private final ObjectProvider<MeterRegistry> meterRegistryProvider;

    /**
     * Creates a new metrics recorder.
     *
     * @param meterRegistryProvider The optional Micrometer registry provider.
     */
    public RecoveryMetricsRecorder(ObjectProvider<MeterRegistry> meterRegistryProvider) {
        this.meterRegistryProvider = meterRegistryProvider;
    }

    /**
     * Records a recovery action outcome.
     *
     * @param entityType The business entity type.
     * @param actionType The governed action.
     * @param outcome The final outcome label.
     */
    public void recordAction(String entityType, RecoveryActionType actionType, String outcome) {
        MeterRegistry meterRegistry = meterRegistryProvider.getIfAvailable();
        if (meterRegistry == null) {
            return;
        }
        Counter.builder("noura.recovery.actions")
                .tag("entityType", entityType)
                .tag("actionType", actionType.name())
                .tag("outcome", outcome)
                .register(meterRegistry)
                .increment();
    }
}
