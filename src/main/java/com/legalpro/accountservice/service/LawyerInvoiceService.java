package com.legalpro.accountservice.service;

import com.legalpro.accountservice.dto.LawyerInvoiceDto;
import java.util.List;
import java.util.UUID;

public interface LawyerInvoiceService {
    LawyerInvoiceDto createInvoice(LawyerInvoiceDto dto, UUID lawyerUuid);
    LawyerInvoiceDto updateInvoice(UUID invoiceUuid, LawyerInvoiceDto dto, UUID lawyerUuid);
    LawyerInvoiceDto getInvoice(UUID invoiceUuid, UUID lawyerUuid);
    List<LawyerInvoiceDto> getInvoicesForLawyer(UUID lawyerUuid);
    void deleteInvoice(UUID invoiceUuid, UUID lawyerUuid);
    List<LawyerInvoiceDto> getInvoicesByStatus(UUID lawyerUuid, String status);
    void updateStripeSessionInfo(UUID invoiceUuid, UUID lawyerUuid, String stripeSessionId, String stripePaymentStatus);
    void markInvoicePaid(String invoiceUuid, String stripeSessionId, String stripePaymentStatus, String activity);
    void markInvoiceFailedByIntent(String paymentIntentId);

}
