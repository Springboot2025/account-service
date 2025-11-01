package com.legalpro.accountservice.mapper;

import com.legalpro.accountservice.dto.ClientInvoiceDto;
import com.legalpro.accountservice.entity.ClientInvoice;
import org.springframework.stereotype.Component;

@Component
public class ClientInvoiceMapper {

    /**
     * Converts ClientInvoice entity to DTO.
     */
    public ClientInvoiceDto toDto(ClientInvoice entity) {
        if (entity == null) return null;

        return ClientInvoiceDto.builder()
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
    public ClientInvoice toEntity(ClientInvoiceDto dto) {
        if (dto == null) return null;

        ClientInvoice.ClientInvoiceBuilder builder = ClientInvoice.builder()
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
