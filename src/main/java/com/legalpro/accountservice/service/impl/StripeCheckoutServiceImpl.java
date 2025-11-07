package com.legalpro.accountservice.service.impl;

import com.legalpro.accountservice.entity.Invoice;
import com.legalpro.accountservice.entity.StripeAccount;
import com.legalpro.accountservice.repository.InvoiceRepository;
import com.legalpro.accountservice.repository.StripeAccountRepository;
import com.legalpro.accountservice.service.StripeCheckoutService;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripeCheckoutServiceImpl implements StripeCheckoutService {

    private final InvoiceRepository invoiceRepository;
    private final StripeAccountRepository stripeAccountRepository;

    @Override
    public String createCheckoutSession(UUID invoiceUuid, UUID clientUuid) {

        // 1) Load invoice
        Invoice invoice = invoiceRepository.findByUuid(invoiceUuid)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        // 2) Validate invoice belongs to this client
        if (!invoice.getClientUuid().equals(clientUuid)) {
            throw new RuntimeException("Unauthorized: Invoice does not belong to this client.");
        }

        // 3) Load Stripe Connect account for lawyer
        StripeAccount sa = stripeAccountRepository.findByLawyerUuid(invoice.getLawyerUuid())
                .orElseThrow(() -> new RuntimeException("Lawyer has not completed Stripe onboarding"));

        // 4) Convert amount to cents
        Long amountCents = invoice.getAmountRequested()
                .multiply(BigDecimal.valueOf(100))
                .longValue();

        // 5) Create Stripe Checkout Session
        try {
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl("https://lawproject-nu.vercel.app/pay/success?invoice=" + invoiceUuid)
                    .setCancelUrl("https://lawproject-nu.vercel.app/pay/cancel")
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency("aud")
                                                    .setUnitAmount(amountCents)
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName("Trust Deposit for Case " + invoice.getCaseUuid())
                                                                    .build()
                                                    )
                                                    .build()
                                    )
                                    .build()
                    )
                    .setPaymentIntentData(
                            SessionCreateParams.PaymentIntentData.builder()
                                    .setTransferData(
                                            SessionCreateParams.PaymentIntentData.TransferData.builder()
                                                    .setDestination(sa.getStripeAccountId()) // ✅ Directly funds lawyer trust account
                                                    .build()
                                    )
                                    .build()
                    )
                    .build();

            Session session = Session.create(params);

            // Store sessionId for reconciliation
            invoice.setStripeSessionId(session.getId());
            invoiceRepository.save(invoice);

            log.info("✅ Stripe checkout created: invoice={} url={}", invoiceUuid, session.getUrl());
            return session.getUrl();

        } catch (Exception e) {
            log.error("❌ Failed to create checkout session for invoice {}", invoiceUuid, e);
            throw new RuntimeException("Stripe checkout session creation failed", e);
        }
    }
}
