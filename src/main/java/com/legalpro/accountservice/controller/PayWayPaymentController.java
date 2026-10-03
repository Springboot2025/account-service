package com.legalpro.accountservice.controller;

import com.legalpro.accountservice.dto.ApiResponse;
import com.legalpro.accountservice.dto.payway.BillingHistoryItemDto;
import com.legalpro.accountservice.dto.payway.MakePaymentRequest;
import com.legalpro.accountservice.dto.payway.PayWayPaymentResponse;
import com.legalpro.accountservice.security.CustomUserDetails;
import com.legalpro.accountservice.service.PayWayPaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Card payments through PayWay. The payer is always the logged-in lawyer
 * (individual, or firm admin) -- never taken from the request body.
 */
@RestController
@RequestMapping("/api/payway")
@PreAuthorize("hasRole('Lawyer')")
@RequiredArgsConstructor
public class PayWayPaymentController {

    private final PayWayPaymentService paymentService;

    @PostMapping("/payments")
    public ResponseEntity<ApiResponse<PayWayPaymentResponse>> makePayment(
            @Valid @RequestBody MakePaymentRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        PayWayPaymentResponse payment = paymentService.makePayment(userDetails.getUuid(), request);

        return switch (payment.getStatus()) {
            case "APPROVED" -> ResponseEntity.ok(ApiResponse.success(200, "Payment approved", payment));
            case "DECLINED" -> ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                    .body(ApiResponse.error(402, "Payment declined: " + payment.getResponseText(), payment));
            // Still pending at PayWay after polling
            default -> ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(ApiResponse.error(502, payment.getErrorMessage(), payment));
        };
    }

    // Billing history: the logged-in payer's invoices, newest first
    @GetMapping("/invoices")
    public ResponseEntity<ApiResponse<List<BillingHistoryItemDto>>> getBillingHistory(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.success(200, "Billing history fetched",
                paymentService.getBillingHistory(userDetails.getUuid())));
    }
}
