package com.legalpro.accountservice.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.legalpro.accountservice.service.StripeWebhookService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/stripe/webhook")
@RequiredArgsConstructor
@Slf4j
public class StripeWebhookController {

    private final StripeWebhookService stripeWebhookService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping
    public ResponseEntity<String> handleWebhook(HttpServletRequest request) throws IOException {

        String payload = request.getReader().lines().collect(Collectors.joining());
        String sigHeader = request.getHeader("Stripe-Signature");
        String webhookSecret = System.getenv("STRIPE_WEBHOOK_SECRET");

        try {
            Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);

            log.info("🔔 Received Stripe event: {}", event.getType());

            // Delegate handling:
            stripeWebhookService.handleEvent(event);

            return ResponseEntity.ok("✅ Event processed");
        }
        catch (SignatureVerificationException e) {
            log.error("❌ Invalid Stripe webhook signature", e);
            return ResponseEntity.status(400).body("Invalid signature");
        }
        catch (Exception e) {
            log.error("❌ Error while processing Stripe webhook", e);
            return ResponseEntity.status(500).body("Webhook error");
        }
    }
}
