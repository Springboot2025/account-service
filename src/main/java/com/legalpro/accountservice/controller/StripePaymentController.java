package com.legalpro.accountservice.controller;

import com.legalpro.accountservice.service.StripePaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class StripePaymentController {

    private final StripePaymentService stripePaymentService;

    @PostMapping("/trust/checkout")
    public ResponseEntity<?> createCheckout(@RequestParam UUID lawyerUuid,
                                            @RequestParam Long amountCents) {

        String url = stripePaymentService.createTrustDepositCheckout(lawyerUuid, amountCents);
        return ResponseEntity.ok(Map.of("checkoutUrl", url));
    }
}

