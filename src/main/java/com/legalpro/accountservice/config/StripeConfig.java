package com.legalpro.accountservice.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StripeConfig {

    @PostConstruct
    public void init() {
        String secretKey = System.getenv("STRIPE_SECRET_KEY");
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("Missing STRIPE_SECRET_KEY environment variable");
        }
        Stripe.apiKey = secretKey;

        System.out.println("✅ Stripe configured successfully");
    }
}
