package com.noura.platform.commerce.b2b.application;

import com.noura.platform.commerce.b2b.domain.Company;
import com.noura.platform.commerce.b2b.domain.PurchaseOrder;
import com.noura.platform.commerce.notifications.application.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Scheduled jobs for B2B operations.
 */
@Component
@ConditionalOnProperty(name = "app.b2b.enabled", havingValue = "true")
public class B2BScheduler {
    private static final Logger log = LoggerFactory.getLogger(B2BScheduler.class);

    private final B2BService b2bService;
    private final NotificationService notificationService;

    public B2BScheduler(B2BService b2bService, NotificationService notificationService) {
        this.b2bService = b2bService;
        this.notificationService = notificationService;
    }

    /**
     * Check for overdue purchase orders and send reminders.
     * Default: daily at 9 AM.
     */
    @Scheduled(cron = "${app.scheduler.b2b-overdue-check-cron:0 0 9 * * *}")
    public void checkOverdueOrders() {
        log.info("Checking for overdue B2B orders...");

        List<PurchaseOrder> overdueOrders = b2bService.findOverdueOrders();

        for (PurchaseOrder order : overdueOrders) {
            try {
                Company company = order.getCompany();
                String email = company.getEmail();

                if (email != null && !email.isBlank()) {
                    notificationService.sendNotification(
                            email,
                            "Payment Reminder: PO " + order.getPoNumber(),
                            String.format(
                                    "Your purchase order %s has a payment due. " +
                                    "Amount due: $%.2f. Please remit payment at your earliest convenience.",
                                    order.getPoNumber(),
                                    order.getBalanceDue()
                            )
                    );
                    log.info("Sent overdue reminder for PO {} to {}", order.getPoNumber(), email);
                }
            } catch (Exception e) {
                log.error("Failed to send overdue reminder for PO {}: {}", 
                        order.getPoNumber(), e.getMessage());
            }
        }

        log.info("B2B overdue check complete. {} orders overdue.", overdueOrders.size());
    }

    /**
     * Generate monthly statements for B2B companies.
     * Default: 1st of every month at 6 AM.
     */
    @Scheduled(cron = "0 0 6 1 * *")
    public void generateMonthlyStatements() {
        log.info("Generating monthly B2B statements...");

        // TODO: Implement statement generation
        // 1. Find all active companies with balances
        // 2. Generate PDF statements
        // 3. Email to company contacts

        log.info("Monthly statement generation complete.");
    }
}
