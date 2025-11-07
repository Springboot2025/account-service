package com.legalpro.accountservice.service;

import com.legalpro.accountservice.dto.InvoiceDto;
import java.util.List;
import java.util.UUID;

public interface InvoiceService {
    InvoiceDto createInvoice(InvoiceDto dto, UUID lawyerUuid);
    InvoiceDto updateInvoice(UUID invoiceUuid, InvoiceDto dto, UUID lawyerUuid);
    InvoiceDto getInvoice(UUID invoiceUuid, UUID lawyerUuid);
    InvoiceDto getInvoiceClient(UUID invoiceUuid, UUID clientUuid);
    List<InvoiceDto> getInvoicesForLawyer(UUID lawyerUuid);
    List<InvoiceDto> getInvoicesForClient(UUID clientUuid);
    void deleteInvoice(UUID invoiceUuid, UUID lawyerUuid);
    List<InvoiceDto> getInvoicesByStatus(UUID lawyerUuid, String status);
    void updateStripeSessionInfo(UUID invoiceUuid, UUID lawyerUuid, String stripeSessionId, String stripePaymentStatus);
    void markInvoicePaid(String invoiceUuid, String stripeSessionId, String stripePaymentStatus, String activity);
    void markInvoiceFailedByIntent(String paymentIntentId);

}
