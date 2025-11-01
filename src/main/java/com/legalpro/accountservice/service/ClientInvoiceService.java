package com.legalpro.accountservice.service;

import com.legalpro.accountservice.dto.ClientInvoiceDto;
import java.util.List;
import java.util.UUID;

public interface ClientInvoiceService {
    ClientInvoiceDto createInvoice(ClientInvoiceDto dto, UUID lawyerUuid);
    ClientInvoiceDto updateInvoice(UUID invoiceUuid, ClientInvoiceDto dto, UUID lawyerUuid);
    ClientInvoiceDto getInvoice(UUID invoiceUuid, UUID lawyerUuid);
    List<ClientInvoiceDto> getInvoicesForLawyer(UUID lawyerUuid);
    void deleteInvoice(UUID invoiceUuid, UUID lawyerUuid);
    List<ClientInvoiceDto> getInvoicesByStatus(UUID lawyerUuid, String status);
    void updateStripeSessionInfo(UUID invoiceUuid, UUID lawyerUuid, String stripeSessionId, String stripePaymentStatus);

}
