package com.noura.platform.inventory.repository;

import com.noura.platform.inventory.domain.WebhookSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WebhookSubscriptionRepository extends JpaRepository<WebhookSubscription, String> {

    List<WebhookSubscription> findAllByActiveTrueAndEventCodeIgnoreCase(String eventCode);
}
