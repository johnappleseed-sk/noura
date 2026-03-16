package com.noura.platform.commerce.notifications.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Registry to manage and select email provider implementations.
 * Automatically selects the highest priority enabled provider.
 */
@Service
public class EmailProviderRegistry {
    private static final Logger log = LoggerFactory.getLogger(EmailProviderRegistry.class);

    private final List<EmailProvider> providers;
    private final EmailProvider primaryProvider;

    public EmailProviderRegistry(List<EmailProvider> providerList) {
        // Sort by priority (lower = higher priority)
        this.providers = providerList.stream()
                .sorted(Comparator.comparingInt(EmailProvider::getPriority))
                .toList();

        // Select first enabled provider as primary
        this.primaryProvider = providers.stream()
                .filter(EmailProvider::isEnabled)
                .findFirst()
                .orElse(null);

        if (primaryProvider != null) {
            log.info("Primary email provider: {} (priority: {})",
                    primaryProvider.getProviderId(), primaryProvider.getPriority());
        } else {
            log.warn("No email provider is enabled - emails will not be sent");
        }
    }

    /**
     * Get the primary (highest priority enabled) email provider.
     */
    public Optional<EmailProvider> getPrimaryProvider() {
        return Optional.ofNullable(primaryProvider);
    }

    /**
     * Get a specific provider by ID.
     */
    public Optional<EmailProvider> getProvider(String providerId) {
        if (providerId == null || providerId.isBlank()) {
            return getPrimaryProvider();
        }
        return providers.stream()
                .filter(p -> p.getProviderId().equalsIgnoreCase(providerId))
                .filter(EmailProvider::isEnabled)
                .findFirst();
    }

    /**
     * Get all enabled providers in priority order.
     */
    public List<EmailProvider> getEnabledProviders() {
        return providers.stream()
                .filter(EmailProvider::isEnabled)
                .toList();
    }

    /**
     * Check if any email provider is available.
     */
    public boolean hasProvider() {
        return primaryProvider != null;
    }

    /**
     * Send email using the primary provider, with fallback to others on failure.
     */
    public EmailProvider.SendResult sendWithFallback(EmailProvider.SendEmailRequest request) {
        List<EmailProvider> enabledProviders = getEnabledProviders();

        if (enabledProviders.isEmpty()) {
            log.warn("No email providers available to send email to {}", request.toEmail());
            return EmailProvider.SendResult.failure("NO_PROVIDER", "No email provider is configured");
        }

        for (EmailProvider provider : enabledProviders) {
            EmailProvider.SendResult result = provider.send(request);
            if (result.success()) {
                return result;
            }
            log.warn("Email provider {} failed, trying next: {} - {}",
                    provider.getProviderId(), result.errorCode(), result.errorMessage());
        }

        return EmailProvider.SendResult.failure("ALL_PROVIDERS_FAILED",
                "All email providers failed to send the message");
    }
}
