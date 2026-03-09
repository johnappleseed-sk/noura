package com.noura.platform.inventory.api;

import com.noura.platform.common.api.ApiResponse;
import com.noura.platform.inventory.dto.webhook.WebhookSubscriptionRequest;
import com.noura.platform.inventory.dto.webhook.WebhookSubscriptionResponse;
import com.noura.platform.inventory.service.WebhookSubscriptionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/inventory/v1/webhooks")
public class WebhookSubscriptionController {

    private final WebhookSubscriptionService webhookSubscriptionService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<WebhookSubscriptionResponse>> listSubscriptions(HttpServletRequest http) {
        return ApiResponse.ok("Webhook subscriptions", webhookSubscriptionService.listSubscriptions(), http.getRequestURI());
    }

    @GetMapping("/{subscriptionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<WebhookSubscriptionResponse> getSubscription(@PathVariable String subscriptionId, HttpServletRequest http) {
        return ApiResponse.ok("Webhook subscription", webhookSubscriptionService.getSubscription(subscriptionId), http.getRequestURI());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<WebhookSubscriptionResponse>> createSubscription(@Valid @RequestBody WebhookSubscriptionRequest request,
                                                                                        HttpServletRequest http) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Webhook subscription created", webhookSubscriptionService.createSubscription(request), http.getRequestURI()));
    }

    @PutMapping("/{subscriptionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<WebhookSubscriptionResponse> updateSubscription(@PathVariable String subscriptionId,
                                                                       @Valid @RequestBody WebhookSubscriptionRequest request,
                                                                       HttpServletRequest http) {
        return ApiResponse.ok("Webhook subscription updated", webhookSubscriptionService.updateSubscription(subscriptionId, request), http.getRequestURI());
    }

    @DeleteMapping("/{subscriptionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteSubscription(@PathVariable String subscriptionId, HttpServletRequest http) {
        webhookSubscriptionService.deleteSubscription(subscriptionId);
        return ApiResponse.ok("Webhook subscription deleted", null, http.getRequestURI());
    }
}
