package com.legalpro.accountservice.service.impl;

import com.legalpro.accountservice.dto.InvoiceDto;
import com.legalpro.accountservice.entity.Invoice;
import com.legalpro.accountservice.entity.LegalCase;
import com.legalpro.accountservice.mapper.InvoiceMapper;
import com.legalpro.accountservice.repository.InvoiceRepository;
import com.legalpro.accountservice.repository.LegalCaseRepository;
import com.legalpro.accountservice.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final LegalCaseRepository legalCaseRepository;
    private final InvoiceMapper mapper;

    @Override
    public InvoiceDto createInvoice(InvoiceDto dto, UUID lawyerUuid) {
        // pull trust balance from linked case
        LegalCase legalCase = legalCaseRepository.findByUuid(dto.getCaseUuid())
                .orElseThrow(() -> new IllegalArgumentException("Case not found"));

        if (!legalCase.getLawyerUuid().equals(lawyerUuid)) {
            throw new SecurityException("You can only create invoices for your own cases");
        }

        BigDecimal trustBalance = legalCase.getAvailableTrustFunds() != null
                ? legalCase.getAvailableTrustFunds()
                : BigDecimal.ZERO;

        Invoice invoice = Invoice.builder()
                .uuid(UUID.randomUUID())
                .caseUuid(dto.getCaseUuid())
                .clientUuid(dto.getClientUuid())
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
    public InvoiceDto updateInvoice(UUID invoiceUuid, InvoiceDto dto, UUID lawyerUuid) {
        Invoice invoice = invoiceRepository.findByUuid(invoiceUuid)
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
    public InvoiceDto getInvoice(UUID invoiceUuid, UUID lawyerUuid) {
        Invoice invoice = invoiceRepository.findByUuid(invoiceUuid)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));
        if (!invoice.getLawyerUuid().equals(lawyerUuid)) {
            throw new SecurityException("Access denied");
        }
        return mapper.toDto(invoice);
    }

    @Override
    public List<InvoiceDto> getInvoicesForLawyer(UUID lawyerUuid) {
        return invoiceRepository.findByLawyerUuid(lawyerUuid)
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<InvoiceDto> getInvoicesForClient(UUID clientUuid) {
        return invoiceRepository.findByClientUuid(clientUuid)
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteInvoice(UUID invoiceUuid, UUID lawyerUuid) {
        Invoice invoice = invoiceRepository.findByUuid(invoiceUuid)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));
        if (!invoice.getLawyerUuid().equals(lawyerUuid)) {
            throw new SecurityException("You can only delete your own invoices");
        }
        invoice.setStatus("DELETED");
        invoice.setDeletedAt(LocalDateTime.now());
        invoiceRepository.save(invoice);
    }

    @Override
    public List<InvoiceDto> getInvoicesByStatus(UUID lawyerUuid, String status) {
        return invoiceRepository.findByLawyerUuid(lawyerUuid)
                .stream()
                .filter(inv -> status.equalsIgnoreCase(inv.getStatus()))
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public void updateStripeSessionInfo(UUID invoiceUuid, UUID lawyerUuid, String stripeSessionId, String stripePaymentStatus) {
        Invoice invoice = invoiceRepository.findByUuidAndLawyerUuid(invoiceUuid, lawyerUuid)
                .orElseThrow(() -> new RuntimeException("Invoice not found for lawyer"));

        invoice.setStripeSessionId(stripeSessionId);
        invoice.setStripePaymentStatus(stripePaymentStatus);
        invoice.setUpdatedAt(OffsetDateTime.now().toLocalDateTime());
        invoice.setLastActivity("Stripe payment session created");
        invoiceRepository.save(invoice);
    }

    @Override
    public void markInvoicePaid(String invoiceUuid, String stripeSessionId, String stripePaymentStatus, String activity) {
        UUID uuid = UUID.fromString(invoiceUuid);
        Invoice invoice = invoiceRepository.findByUuid(uuid)
                .orElseThrow(() -> new RuntimeException("Invoice not found for webhook"));

        invoice.setStripeSessionId(stripeSessionId);
        invoice.setStripePaymentStatus(stripePaymentStatus);
        invoice.setStatus("PAID");
        invoice.setLastActivity(activity);
        invoice.setUpdatedAt(LocalDateTime.now());
        invoiceRepository.save(invoice);

        log.info("✅ Invoice {} marked as PAID (session {})", invoiceUuid, stripeSessionId);
    }

    @Override
    public void markInvoiceFailedByIntent(String paymentIntentId) {
        // Optional: if you later store PaymentIntent IDs
        log.warn("Payment failed for intent {}", paymentIntentId);
    }

}
