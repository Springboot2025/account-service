package com.legalpro.accountservice.config;

import org.springframework.stereotype.Component;

@Component
public class StripeKeys {

    public String getSecretKey() {
        return System.getenv("STRIPE_SECRET_KEY");
    }

    public String getPublishableKey() {
        return System.getenv("STRIPE_PUBLISHABLE_KEY");
    }

    public String getWebhookSecret() {
        return System.getenv("STRIPE_WEBHOOK_SECRET");
    }
}
