package com.legalpro.accountservice.controller;

import com.legalpro.accountservice.dto.ClientInvoiceDto;
import com.legalpro.accountservice.service.ClientInvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/lawyer/invoices")
@PreAuthorize("hasRole('Lawyer')")
@RequiredArgsConstructor
public class ClientInvoiceController {

    private final ClientInvoiceService clientInvoiceService;

    /**
     * Helper to extract the current lawyer's UUID from the Principal.
     * Adjust if your SecurityContext stores UUID differently.
     */
    private UUID getLoggedLawyerUuid(Principal principal) {
        // Example: if principal.getName() contains UUID string
        return UUID.fromString(principal.getName());
    }

    // ----------------------------------------------------------------------
    // CREATE
    // ----------------------------------------------------------------------
    @PostMapping
    public ClientInvoiceDto createInvoice(@RequestBody ClientInvoiceDto dto, Principal principal) {
        UUID lawyerUuid = getLoggedLawyerUuid(principal);
        log.info("Creating invoice for case {} by lawyer {}", dto.getCaseUuid(), lawyerUuid);
        return clientInvoiceService.createInvoice(dto, lawyerUuid);
    }

    // ----------------------------------------------------------------------
    // UPDATE
    // ----------------------------------------------------------------------
    @PutMapping("/{invoiceUuid}")
    public ClientInvoiceDto updateInvoice(
            @PathVariable UUID invoiceUuid,
            @RequestBody ClientInvoiceDto dto,
            Principal principal
    ) {
        UUID lawyerUuid = getLoggedLawyerUuid(principal);
        log.info("Updating invoice {} by lawyer {}", invoiceUuid, lawyerUuid);
        return clientInvoiceService.updateInvoice(invoiceUuid, dto, lawyerUuid);
    }

    // ----------------------------------------------------------------------
    // GET SINGLE
    // ----------------------------------------------------------------------
    @GetMapping("/{invoiceUuid}")
    public ClientInvoiceDto getInvoice(
            @PathVariable UUID invoiceUuid,
            Principal principal
    ) {
        UUID lawyerUuid = getLoggedLawyerUuid(principal);
        return clientInvoiceService.getInvoice(invoiceUuid, lawyerUuid);
    }

    // ----------------------------------------------------------------------
    // GET ALL for logged-in lawyer
    // ----------------------------------------------------------------------
    @GetMapping
    public List<ClientInvoiceDto> getInvoices(Principal principal) {
        UUID lawyerUuid = getLoggedLawyerUuid(principal);
        return clientInvoiceService.getInvoicesForLawyer(lawyerUuid);
    }

    // ----------------------------------------------------------------------
    // GET by status
    // ----------------------------------------------------------------------
    @GetMapping("/status/{status}")
    public List<ClientInvoiceDto> getInvoicesByStatus(
            @PathVariable String status,
            Principal principal
    ) {
        UUID lawyerUuid = getLoggedLawyerUuid(principal);
        return clientInvoiceService.getInvoicesByStatus(lawyerUuid, status);
    }

    // ----------------------------------------------------------------------
    // DELETE (soft delete)
    // ----------------------------------------------------------------------
    @DeleteMapping("/{invoiceUuid}")
    public void deleteInvoice(
            @PathVariable UUID invoiceUuid,
            Principal principal
    ) {
        UUID lawyerUuid = getLoggedLawyerUuid(principal);
        log.info("Deleting invoice {} by lawyer {}", invoiceUuid, lawyerUuid);
        clientInvoiceService.deleteInvoice(invoiceUuid, lawyerUuid);
    }
}
