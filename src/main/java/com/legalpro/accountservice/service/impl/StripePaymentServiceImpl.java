package com.legalpro.accountservice.service.impl;

import com.legalpro.accountservice.entity.StripeAccount;
import com.legalpro.accountservice.repository.StripeAccountRepository;
import com.legalpro.accountservice.service.StripePaymentService;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripePaymentServiceImpl implements StripePaymentService {

    private final StripeAccountRepository stripeAccountRepository;

    @Override
    public String createTrustDepositCheckout(UUID lawyerUuid, Long amountCents) {

        StripeAccount sa = stripeAccountRepository.findByLawyerUuid(lawyerUuid)
                .orElseThrow(() -> new RuntimeException("Lawyer does not have a Stripe account yet"));

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl("https://lawproject-nu.vercel.app/pay/success?lawyer=" + lawyerUuid)
                .setCancelUrl("https://lawproject-nu.vercel.app/pay/cancel")
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency("aud")
                                                .setUnitAmount(amountCents) // e.g., 5000 = $50.00
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName("Trust Deposit to Lawyer")
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
                                                .setDestination(sa.getStripeAccountId()) // ✅ Moves money directly to lawyer trust account
                                                .build()
                                )
                                .build()
                )
                .build();

        try {
            Session session = Session.create(params);
            log.info("✅ Created Checkout session {} for lawyer {}", session.getId(), lawyerUuid);
            return session.getUrl();
        } catch (Exception e) {
            log.error("❌ Failed to create Checkout session", e);
            throw new RuntimeException("Could not create trust deposit checkout session", e);
        }
    }
}
