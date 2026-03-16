package com.noura.platform.config;

import com.noura.platform.domain.enums.RecoveryActionType;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

/**
 * Binds configuration that controls recovery-center tenant scoping and retention defaults.
 */
@Component
@ConfigurationProperties(prefix = "app.recovery")
public class RecoveryProperties {

    private String defaultTenantKey = "default";

    private int defaultRetentionDays = 30;

    /**
     * Minimum reason length required for destructive actions and approval requests.
     */
    private int minReasonLength = 12;

    /**
     * Action types that require a 4-eyes approval workflow before execution.
     */
    private Set<RecoveryActionType> approvalRequiredActions = EnumSet.of(
            RecoveryActionType.TRASH,
            RecoveryActionType.HARD_DELETE,
            RecoveryActionType.ANONYMIZE
    );

    private Alerts alerts = new Alerts();

    /**
     * Returns the default tenant key used when no explicit tenant header is supplied.
     *
     * @return The default tenant key.
     */
    public String getDefaultTenantKey() {
        return defaultTenantKey;
    }

    /**
     * Updates the default tenant key used when no explicit tenant header is supplied.
     *
     * @param defaultTenantKey The default tenant key.
     */
    public void setDefaultTenantKey(String defaultTenantKey) {
        this.defaultTenantKey = defaultTenantKey;
    }

    /**
     * Returns the default retention window, in days, for trashed records.
     *
     * @return The default retention window in days.
     */
    public int getDefaultRetentionDays() {
        return defaultRetentionDays;
    }

    /**
     * Updates the default retention window, in days, for trashed records.
     *
     * @param defaultRetentionDays The default retention window in days.
     */
    public void setDefaultRetentionDays(int defaultRetentionDays) {
        this.defaultRetentionDays = defaultRetentionDays;
    }

    public int getMinReasonLength() {
        return minReasonLength;
    }

    public void setMinReasonLength(int minReasonLength) {
        this.minReasonLength = minReasonLength;
    }

    public Set<RecoveryActionType> getApprovalRequiredActions() {
        return approvalRequiredActions;
    }

    public void setApprovalRequiredActions(Set<RecoveryActionType> approvalRequiredActions) {
        if (approvalRequiredActions == null || approvalRequiredActions.isEmpty()) {
            this.approvalRequiredActions = EnumSet.noneOf(RecoveryActionType.class);
            return;
        }
        this.approvalRequiredActions = EnumSet.copyOf(approvalRequiredActions);
    }

    public Alerts getAlerts() {
        return alerts;
    }

    public void setAlerts(Alerts alerts) {
        this.alerts = alerts == null ? new Alerts() : alerts;
    }

    public static class Alerts {
        /**
         * Enables outbound escalation notifications for high-impact incidents.
         */
        private boolean enabled = false;

        /**
         * Slack incoming webhook URL used for incident escalation.
         */
        private String slackWebhookUrl;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getSlackWebhookUrl() {
            return slackWebhookUrl;
        }

        public void setSlackWebhookUrl(String slackWebhookUrl) {
            this.slackWebhookUrl = slackWebhookUrl;
        }
    }
}
