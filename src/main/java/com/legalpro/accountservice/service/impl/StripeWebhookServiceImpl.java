package com.legalpro.accountservice.service.impl;

import com.legalpro.accountservice.entity.StripeAccount;
import com.legalpro.accountservice.repository.StripeAccountRepository;
import com.legalpro.accountservice.service.StripeWebhookService;
import com.stripe.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripeWebhookServiceImpl implements StripeWebhookService {

    private final StripeAccountRepository stripeAccountRepository;

    @Override
    public void handleEvent(Event event) {

        switch (event.getType()) {

            case "account.updated" -> handleAccountUpdated(event);

            case "payment_intent.succeeded" -> handlePaymentSucceeded(event);

            case "payout.paid" -> handlePayoutPaid(event);

            default -> log.debug("Ignoring event type: {}", event.getType());
        }
    }

    private void handleAccountUpdated(Event event) {
        Account account = (Account) event.getDataObjectDeserializer()
                .getObject().orElse(null);
        if (account == null) return;

        StripeAccount sa = stripeAccountRepository.findByStripeAccountId(account.getId())
                .orElse(null);
        if (sa == null) return;

        sa.setChargesEnabled(Boolean.TRUE.equals(account.getChargesEnabled()));
        sa.setPayoutsEnabled(Boolean.TRUE.equals(account.getPayoutsEnabled()));
        stripeAccountRepository.save(sa);

        log.info("✅ Updated onboarding status for {} → charges={}, payouts={}",
                account.getId(), account.getChargesEnabled(), account.getPayoutsEnabled());
    }

    private void handlePaymentSucceeded(Event event) {
        PaymentIntent pi = (PaymentIntent) event.getDataObjectDeserializer()
                .getObject().orElse(null);
        if (pi == null) return;

        String connectedAccountId = event.getAccount(); // <-- Lawyer account

        log.info("💰 Client payment succeeded: amount={} connected_account={}",
                pi.getAmount(), connectedAccountId);

        // Next step: insert into trust ledger (we will do after this part)
    }

    private void handlePayoutPaid(Event event) {
        Payout payout = (Payout) event.getDataObjectDeserializer()
                .getObject().orElse(null);
        if (payout == null) return;

        log.info("🏦 Lawyer payout processed: amount={} account={}",
                payout.getAmount(), event.getAccount());

        // Next step: insert into ledger withdrawal entries
    }
}
