package com.legalpro.accountservice.service;

import com.legalpro.accountservice.config.PayWayProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Charges subscriptions whose renews_at has come round (see
 * PayWayPaymentService.renewDueSubscriptions). Runs daily at 02:00 Melbourne
 * time (PAYWAY_RENEWAL_CRON); off unless PAYWAY_RENEWAL_AUTO_ENABLED=true.
 */
@Component
@RequiredArgsConstructor
public class SubscriptionRenewalJob {

    private final PayWayProperties payWayProperties;
    private final PayWayPaymentService paymentService;

    @Scheduled(cron = "${payway.renewal-cron:0 0 2 * * *}", zone = "Australia/Melbourne")
    public void renewDueSubscriptions() {
        if (!payWayProperties.isAutoRenewEnabled() || !payWayProperties.isConfigured()) {
            return;
        }
        paymentService.renewDueSubscriptions();
    }
}
