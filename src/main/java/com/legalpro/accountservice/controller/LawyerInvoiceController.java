package com.legalpro.accountservice.controller;

import com.legalpro.accountservice.dto.InvoiceDto;
import com.legalpro.accountservice.dto.ApiResponse;
import com.legalpro.accountservice.security.CustomUserDetails;
import com.legalpro.accountservice.service.InvoiceService;
import com.legalpro.accountservice.service.StripeCheckoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/lawyer/invoices")
@PreAuthorize("hasRole('Lawyer')")
@RequiredArgsConstructor
public class LawyerInvoiceController {

    private final InvoiceService invoiceService;
    private final StripeCheckoutService stripeCheckoutService;

    // --- Create Invoice ---
    @PostMapping
    public ResponseEntity<ApiResponse<InvoiceDto>> createInvoice(
            @RequestBody InvoiceDto dto,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID lawyerUuid = userDetails.getUuid();
        InvoiceDto created = invoiceService.createInvoice(dto, lawyerUuid);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(201, "Invoice created successfully", created));
    }

    // --- Update Invoice ---
    @PutMapping("/{invoiceUuid}")
    public ResponseEntity<ApiResponse<InvoiceDto>> updateInvoice(
            @PathVariable UUID invoiceUuid,
            @RequestBody InvoiceDto dto,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID lawyerUuid = userDetails.getUuid();
        InvoiceDto updated = invoiceService.updateInvoice(invoiceUuid, dto, lawyerUuid);
        return ResponseEntity.ok(ApiResponse.success(200, "Invoice updated successfully", updated));
    }

    // --- Get Single Invoice ---
    @GetMapping("/{invoiceUuid}")
    public ResponseEntity<ApiResponse<InvoiceDto>> getInvoice(
            @PathVariable UUID invoiceUuid,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID lawyerUuid = userDetails.getUuid();
        InvoiceDto invoice = invoiceService.getInvoice(invoiceUuid, lawyerUuid);
        return ResponseEntity.ok(ApiResponse.success(200, "Invoice fetched successfully", invoice));
    }

    // --- Get All Invoices for Lawyer ---
    @GetMapping
    public ResponseEntity<ApiResponse<List<InvoiceDto>>> getInvoicesForLawyer(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID lawyerUuid = userDetails.getUuid();
        List<InvoiceDto> invoices = invoiceService.getInvoicesForLawyer(lawyerUuid);
        return ResponseEntity.ok(ApiResponse.success(200, "Invoices fetched successfully", invoices));
    }

    // --- Get Invoices by Status ---
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<InvoiceDto>>> getInvoicesByStatus(
            @PathVariable String status,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID lawyerUuid = userDetails.getUuid();
        List<InvoiceDto> invoices = invoiceService.getInvoicesByStatus(lawyerUuid, status);
        return ResponseEntity.ok(ApiResponse.success(200, "Invoices filtered by status", invoices));
    }

    // --- Soft Delete Invoice ---
    @DeleteMapping("/{invoiceUuid}")
    public ResponseEntity<ApiResponse<Void>> deleteInvoice(
            @PathVariable UUID invoiceUuid,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID lawyerUuid = userDetails.getUuid();
        invoiceService.deleteInvoice(invoiceUuid, lawyerUuid);
        return ResponseEntity.ok(ApiResponse.success(200, "Invoice deleted successfully", null));
    }

    /*@PostMapping("/{invoiceUuid}/checkout-session")
    public ResponseEntity<ApiResponse<Map<String, String>>> createStripeSession(
            @PathVariable UUID invoiceUuid,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        try {
            InvoiceDto invoice = invoiceService.getInvoice(invoiceUuid, userDetails.getUuid());
            String url = stripeCheckoutService.createCheckoutSession(
                    invoiceUuid,
                    userDetails.getUuid(),
                    invoice.getAmountRequested()
            );

            return ResponseEntity.ok(
                    ApiResponse.success(200, "Stripe checkout session created", Map.of("checkoutUrl", url))
            );
        } catch (Exception e) {
            log.error("Stripe session creation failed", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(500, "Stripe session creation failed: " + e.getMessage()));
        }
    }*/


}
