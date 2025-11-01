package com.legalpro.accountservice.controller;

import com.legalpro.accountservice.service.ClientInvoiceService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/webhooks/stripe")
@RequiredArgsConstructor
public class StripeWebhookController {

    private final ClientInvoiceService clientInvoiceService;

    @PostMapping
    public ResponseEntity<String> handleStripeEvent(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        String webhookSecret = System.getenv("STRIPE_WEBHOOK_SECRET");
        if (webhookSecret == null || webhookSecret.isBlank()) {
            log.error("❌ Missing STRIPE_WEBHOOK_SECRET env var");
            return ResponseEntity.internalServerError().body("Webhook secret not configured");
        }

        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.error("⚠️ Invalid Stripe signature: {}", e.getMessage());
            return ResponseEntity.status(400).body("Invalid signature");
        } catch (Exception e) {
            log.error("⚠️ Failed to parse Stripe event: {}", e.getMessage());
            return ResponseEntity.status(400).body("Invalid payload");
        }

        log.info("📩 Received Stripe event: {} ({})", event.getType(), event.getId());

        try {
            switch (event.getType()) {
                case "checkout.session.completed" -> {
                    Session session = (Session) event.getDataObjectDeserializer()
                            .getObject()
                            .orElse(null);
                    if (session != null) {
                        String invoiceUuid = session.getMetadata().get("invoice_uuid");
                        log.info("✅ Checkout completed for invoice: {}", invoiceUuid);
                        clientInvoiceService.markInvoicePaid(invoiceUuid,
                                session.getId(),
                                "SUCCEEDED",
                                "Client payment received via Stripe");
                    }
                }

                case "payment_intent.payment_failed" -> {
                    com.stripe.model.PaymentIntent paymentIntent =
                            (com.stripe.model.PaymentIntent) event.getDataObjectDeserializer()
                                    .getObject()
                                    .orElse(null);

                    if (paymentIntent != null) {
                        String paymentIntentId = paymentIntent.getId();
                        log.warn("⚠️ Payment failed for intent {}", paymentIntentId);
                        clientInvoiceService.markInvoiceFailedByIntent(paymentIntentId);
                    } else {
                        log.warn("⚠️ Received payment_intent.payment_failed but unable to deserialize object");
                    }
                }

                default -> log.info("ℹ️ Ignoring unhandled event type: {}", event.getType());
            }
        } catch (Exception e) {
            log.error("❌ Error processing Stripe event: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error processing event");
        }

        return ResponseEntity.ok("Event processed");
    }
}
