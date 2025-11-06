package com.legalpro.accountservice.controller;

import com.legalpro.accountservice.dto.ApiResponse;
import com.legalpro.accountservice.security.CustomUserDetails;
import com.legalpro.accountservice.service.LawyerInvoiceService;
import com.legalpro.accountservice.service.StripeCheckoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/client/invoices")
@PreAuthorize("hasRole('Client')")
@RequiredArgsConstructor
public class ClientInvoiceController {

    private final LawyerInvoiceService clientInvoiceService;
    private final StripeCheckoutService stripeCheckoutService;

    // Get all invoices for this client
    @GetMapping
    public ResponseEntity<?> getMyInvoices(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(
                ApiResponse.success(200, "Invoices fetched",
                        clientInvoiceService.getInvoicesForClient(userDetails.getUuid()))
        );
    }

    // Pay invoice (this triggers Stripe Checkout)
    @PostMapping("/{invoiceUuid}/pay")
    public ResponseEntity<?> payInvoice(
            @PathVariable UUID invoiceUuid,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        String checkoutUrl = stripeCheckoutService.createCheckoutSession(invoiceUuid, userDetails.getUuid());
        return ResponseEntity.ok(
                ApiResponse.success(200, "Checkout session created", Map.of("checkoutUrl", checkoutUrl))
        );
    }
}

