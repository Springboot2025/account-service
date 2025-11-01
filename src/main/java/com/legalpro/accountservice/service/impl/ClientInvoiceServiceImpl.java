package com.legalpro.accountservice.service.impl;

import com.legalpro.accountservice.dto.ClientInvoiceDto;
import com.legalpro.accountservice.entity.ClientInvoice;
import com.legalpro.accountservice.entity.LegalCase;
import com.legalpro.accountservice.mapper.ClientInvoiceMapper;
import com.legalpro.accountservice.repository.ClientInvoiceRepository;
import com.legalpro.accountservice.repository.LegalCaseRepository;
import com.legalpro.accountservice.service.ClientInvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ClientInvoiceServiceImpl implements ClientInvoiceService {

    private final ClientInvoiceRepository invoiceRepository;
    private final LegalCaseRepository legalCaseRepository;
    private final ClientInvoiceMapper mapper;

    @Override
    public ClientInvoiceDto createInvoice(ClientInvoiceDto dto, UUID lawyerUuid) {
        // pull trust balance from linked case
        LegalCase legalCase = legalCaseRepository.findByUuid(dto.getCaseUuid())
                .orElseThrow(() -> new IllegalArgumentException("Case not found"));

        if (!legalCase.getLawyerUuid().equals(lawyerUuid)) {
            throw new SecurityException("You can only create invoices for your own cases");
        }

        BigDecimal trustBalance = legalCase.getAvailableTrustFunds() != null
                ? legalCase.getAvailableTrustFunds()
                : BigDecimal.ZERO;

        ClientInvoice invoice = ClientInvoice.builder()
                .uuid(UUID.randomUUID())
                .caseUuid(dto.getCaseUuid())
                .lawyerUuid(lawyerUuid)
                .trustBalance(trustBalance)
                .amountRequested(dto.getAmountRequested())
                .dueDate(dto.getDueDate())
                .lastActivity("Created")
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        invoiceRepository.save(invoice);
        return mapper.toDto(invoice);
    }

    @Override
    public ClientInvoiceDto updateInvoice(UUID invoiceUuid, ClientInvoiceDto dto, UUID lawyerUuid) {
        ClientInvoice invoice = invoiceRepository.findByUuid(invoiceUuid)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));

        if (!invoice.getLawyerUuid().equals(lawyerUuid)) {
            throw new SecurityException("You can only update your own invoices");
        }

        if (dto.getAmountRequested() != null)
            invoice.setAmountRequested(dto.getAmountRequested());
        if (dto.getDueDate() != null)
            invoice.setDueDate(dto.getDueDate());
        if (dto.getStatus() != null)
            invoice.setStatus(dto.getStatus());
        if (dto.getLastActivity() != null)
            invoice.setLastActivity(dto.getLastActivity());

        invoice.setUpdatedAt(LocalDateTime.now());
        invoiceRepository.save(invoice);
        return mapper.toDto(invoice);
    }

    @Override
    public ClientInvoiceDto getInvoice(UUID invoiceUuid, UUID lawyerUuid) {
        ClientInvoice invoice = invoiceRepository.findByUuid(invoiceUuid)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));
        if (!invoice.getLawyerUuid().equals(lawyerUuid)) {
            throw new SecurityException("Access denied");
        }
        return mapper.toDto(invoice);
    }

    @Override
    public List<ClientInvoiceDto> getInvoicesForLawyer(UUID lawyerUuid) {
        return invoiceRepository.findByLawyerUuid(lawyerUuid)
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteInvoice(UUID invoiceUuid, UUID lawyerUuid) {
        ClientInvoice invoice = invoiceRepository.findByUuid(invoiceUuid)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));
        if (!invoice.getLawyerUuid().equals(lawyerUuid)) {
            throw new SecurityException("You can only delete your own invoices");
        }
        invoice.setStatus("DELETED");
        invoice.setDeletedAt(LocalDateTime.now());
        invoiceRepository.save(invoice);
    }

    @Override
    public List<ClientInvoiceDto> getInvoicesByStatus(UUID lawyerUuid, String status) {
        return invoiceRepository.findByLawyerUuid(lawyerUuid)
                .stream()
                .filter(inv -> status.equalsIgnoreCase(inv.getStatus()))
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public void updateStripeSessionInfo(UUID invoiceUuid, UUID lawyerUuid, String stripeSessionId, String stripePaymentStatus) {
        ClientInvoice invoice = invoiceRepository.findByUuidAndLawyerUuid(invoiceUuid, lawyerUuid)
                .orElseThrow(() -> new RuntimeException("Invoice not found for lawyer"));

        invoice.setStripeSessionId(stripeSessionId);
        invoice.setStripePaymentStatus(stripePaymentStatus);
        invoice.setUpdatedAt(OffsetDateTime.now().toLocalDateTime());
        invoice.setLastActivity("Stripe payment session created");
        invoiceRepository.save(invoice);
    }

}
