package com.legalpro.accountservice.mapper;

import com.legalpro.accountservice.dto.InvoiceDto;
import com.legalpro.accountservice.entity.Invoice;
import org.springframework.stereotype.Component;

@Component
public class InvoiceMapper {

    /**
     * Converts ClientInvoice entity to DTO.
     */
    public InvoiceDto toDto(Invoice entity) {
        if (entity == null) return null;

        return InvoiceDto.builder()
                .id(entity.getId())
                .uuid(entity.getUuid())
                .caseUuid(entity.getCaseUuid())
                .lawyerUuid(entity.getLawyerUuid())
                .trustBalance(entity.getTrustBalance())
                .amountRequested(entity.getAmountRequested())
                .dueDate(entity.getDueDate())
                .lastActivity(entity.getLastActivity())
                .status(entity.getStatus())
                .stripeSessionId(entity.getStripeSessionId())          // ✅ new
                .stripePaymentStatus(entity.getStripePaymentStatus())  // ✅ new
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .deletedAt(entity.getDeletedAt())
                .build();
    }

    /**
     * Converts ClientInvoice DTO to entity.
     * Service layer should handle any case or lawyer lookups.
     */
    public Invoice toEntity(InvoiceDto dto) {
        if (dto == null) return null;

        Invoice.InvoiceBuilder builder = Invoice.builder()
                .id(dto.getId())
                .uuid(dto.getUuid())
                .caseUuid(dto.getCaseUuid())
                .lawyerUuid(dto.getLawyerUuid())
                .trustBalance(dto.getTrustBalance())
                .amountRequested(dto.getAmountRequested())
                .dueDate(dto.getDueDate())
                .lastActivity(dto.getLastActivity())
                .status(dto.getStatus())
                .stripeSessionId(dto.getStripeSessionId())            // ✅ new
                .stripePaymentStatus(dto.getStripePaymentStatus())    // ✅ new
                .createdAt(dto.getCreatedAt())
                .updatedAt(dto.getUpdatedAt())
                .deletedAt(dto.getDeletedAt());

        return builder.build();
    }
}
