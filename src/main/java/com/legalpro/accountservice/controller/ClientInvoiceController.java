package com.legalpro.accountservice.controller;

import com.legalpro.accountservice.dto.ApiResponse;
import com.legalpro.accountservice.dto.InvoiceDto;
import com.legalpro.accountservice.security.CustomUserDetails;
import com.legalpro.accountservice.service.InvoiceService;
import com.legalpro.accountservice.service.StripeCheckoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/client/invoices")
@PreAuthorize("hasRole('Client')")
@RequiredArgsConstructor
public class ClientInvoiceController {

    private final InvoiceService invoiceService;
    private final StripeCheckoutService stripeCheckoutService;

    // Get all invoices for this client
    @GetMapping
    public ResponseEntity<?> getMyInvoices(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(
                ApiResponse.success(200, "Invoices fetched",
                        invoiceService.getInvoicesForClient(userDetails.getUuid()))
        );
    }

    @GetMapping("/{invoiceUuid}")
    public ResponseEntity<ApiResponse<InvoiceDto>> getInvoice(
            @PathVariable UUID invoiceUuid,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID clientUuid = userDetails.getUuid();
        InvoiceDto invoice = invoiceService.getInvoiceClient(invoiceUuid, clientUuid);
        return ResponseEntity.ok(ApiResponse.success(200, "Invoice fetched successfully", invoice));
    }
}

