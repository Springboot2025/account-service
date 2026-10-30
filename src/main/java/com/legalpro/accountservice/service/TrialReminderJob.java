package com.legalpro.accountservice.service;

import com.legalpro.accountservice.config.PayWayProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Emails trial subscribers 5 days and 1 day before their first real charge
 * (see PayWayPaymentService.sendTrialReminders). Runs daily at 08:00
 * Melbourne time (PAYWAY_TRIAL_REMINDER_CRON); gated the same way as
 * SubscriptionRenewalJob -- telling someone they're about to be auto-charged
 * when auto-renew isn't actually switched on would be misleading.
 */
@Component
@RequiredArgsConstructor
public class TrialReminderJob {

    private final PayWayProperties payWayProperties;
    private final PayWayPaymentService paymentService;

    @Scheduled(cron = "${payway.trial-reminder-cron:0 0 8 * * *}", zone = "Australia/Melbourne")
    public void sendTrialReminders() {
        if (!payWayProperties.isAutoRenewEnabled() || !payWayProperties.isConfigured()) {
            return;
        }
        paymentService.sendTrialReminders();
    }
}
