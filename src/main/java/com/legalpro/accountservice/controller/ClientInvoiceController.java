package com.legalpro.accountservice.controller;

import com.legalpro.accountservice.dto.ClientInvoiceDto;
import com.legalpro.accountservice.dto.ApiResponse;
import com.legalpro.accountservice.security.CustomUserDetails;
import com.legalpro.accountservice.service.ClientInvoiceService;
import com.legalpro.accountservice.service.StripeCheckoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/lawyer/invoices")
@PreAuthorize("hasRole('Lawyer')")
@RequiredArgsConstructor
public class ClientInvoiceController {

    private final ClientInvoiceService clientInvoiceService;
    private final StripeCheckoutService stripeCheckoutService;

    // --- Create Invoice ---
    @PostMapping
    public ResponseEntity<ApiResponse<ClientInvoiceDto>> createInvoice(
            @RequestBody ClientInvoiceDto dto,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID lawyerUuid = userDetails.getUuid();
        ClientInvoiceDto created = clientInvoiceService.createInvoice(dto, lawyerUuid);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(201, "Invoice created successfully", created));
    }

    // --- Update Invoice ---
    @PutMapping("/{invoiceUuid}")
    public ResponseEntity<ApiResponse<ClientInvoiceDto>> updateInvoice(
            @PathVariable UUID invoiceUuid,
            @RequestBody ClientInvoiceDto dto,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID lawyerUuid = userDetails.getUuid();
        ClientInvoiceDto updated = clientInvoiceService.updateInvoice(invoiceUuid, dto, lawyerUuid);
        return ResponseEntity.ok(ApiResponse.success(200, "Invoice updated successfully", updated));
    }

    // --- Get Single Invoice ---
    @GetMapping("/{invoiceUuid}")
    public ResponseEntity<ApiResponse<ClientInvoiceDto>> getInvoice(
            @PathVariable UUID invoiceUuid,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID lawyerUuid = userDetails.getUuid();
        ClientInvoiceDto invoice = clientInvoiceService.getInvoice(invoiceUuid, lawyerUuid);
        return ResponseEntity.ok(ApiResponse.success(200, "Invoice fetched successfully", invoice));
    }

    // --- Get All Invoices for Lawyer ---
    @GetMapping
    public ResponseEntity<ApiResponse<List<ClientInvoiceDto>>> getInvoicesForLawyer(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID lawyerUuid = userDetails.getUuid();
        List<ClientInvoiceDto> invoices = clientInvoiceService.getInvoicesForLawyer(lawyerUuid);
        return ResponseEntity.ok(ApiResponse.success(200, "Invoices fetched successfully", invoices));
    }

    // --- Get Invoices by Status ---
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<ClientInvoiceDto>>> getInvoicesByStatus(
            @PathVariable String status,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID lawyerUuid = userDetails.getUuid();
        List<ClientInvoiceDto> invoices = clientInvoiceService.getInvoicesByStatus(lawyerUuid, status);
        return ResponseEntity.ok(ApiResponse.success(200, "Invoices filtered by status", invoices));
    }

    // --- Soft Delete Invoice ---
    @DeleteMapping("/{invoiceUuid}")
    public ResponseEntity<ApiResponse<Void>> deleteInvoice(
            @PathVariable UUID invoiceUuid,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID lawyerUuid = userDetails.getUuid();
        clientInvoiceService.deleteInvoice(invoiceUuid, lawyerUuid);
        return ResponseEntity.ok(ApiResponse.success(200, "Invoice deleted successfully", null));
    }

    @PostMapping("/{invoiceUuid}/checkout-session")
    public ResponseEntity<ApiResponse<Map<String, String>>> createStripeSession(
            @PathVariable UUID invoiceUuid,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        try {
            ClientInvoiceDto invoice = clientInvoiceService.getInvoice(invoiceUuid, userDetails.getUuid());
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
    }


}
