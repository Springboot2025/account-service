package com.legalpro.accountservice.service;

import com.stripe.model.Event;

public interface StripeWebhookService {
    void handleEvent(Event event);
}
