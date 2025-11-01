package com.legalpro.accountservice.service.impl;

import com.legalpro.accountservice.dto.ClientInvoiceDto;
import com.legalpro.accountservice.service.ClientInvoiceService;
import com.legalpro.accountservice.service.StripeCheckoutService;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StripeCheckoutServiceImpl implements StripeCheckoutService {

    private final ClientInvoiceService clientInvoiceService;

    @Override
    public String createCheckoutSession(UUID invoiceUuid, UUID lawyerUuid, BigDecimal amountRequested) throws Exception {
        if (amountRequested == null || amountRequested.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Invalid amount");
        }

        long amountInCents = amountRequested.multiply(BigDecimal.valueOf(100)).longValueExact();

        // Stripe Checkout session configuration
        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setCurrency("aud")
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency("aud")
                                                .setUnitAmount(amountInCents)
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName("Legal Invoice Payment")
                                                                .setDescription("Invoice: " + invoiceUuid)
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                )
                // ✅ Using your requested URLs:
                .setSuccessUrl("https://your-frontend.com/payment-success?invoice=" + invoiceUuid)
                .setCancelUrl("https://your-frontend.com/payment-cancelled?invoice=" + invoiceUuid)
                .putMetadata("invoice_uuid", invoiceUuid.toString())
                .putMetadata("lawyer_uuid", lawyerUuid.toString())
                .build();

        // Create session via Stripe API
        Session session = Session.create(params);

        log.info("✅ Stripe Checkout session created for invoice {} | amount: {} AUD | URL: {}",
                invoiceUuid, amountRequested, session.getUrl());

        return session.getUrl();
    }
}
