package com.legalpro.accountservice.mapper;

import com.legalpro.accountservice.dto.QuoteDto;
import com.legalpro.accountservice.entity.Quote;
import org.springframework.stereotype.Component;

@Component
public class QuoteMapper {

    public QuoteDto toDto(Quote entity) {
        if (entity == null) return null;

        return QuoteDto.builder()
                .id(entity.getId())
                .uuid(entity.getUuid())
                .lawyerUuid(entity.getLawyerUuid())
                .clientUuid(entity.getClientUuid())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .expectedAmount(entity.getExpectedAmount())
                .quotedAmount(entity.getQuotedAmount())
                .currency(entity.getCurrency())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .deletedAt(entity.getDeletedAt())
                .build();
    }

    public Quote toEntity(QuoteDto dto) {
        if (dto == null) return null;

        return Quote.builder()
                .id(dto.getId())
                .uuid(dto.getUuid())
                .lawyerUuid(dto.getLawyerUuid())
                .clientUuid(dto.getClientUuid())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .expectedAmount(dto.getExpectedAmount())
                .quotedAmount(dto.getQuotedAmount())
                .currency(dto.getCurrency())
                .status(dto.getStatus())
                .createdAt(dto.getCreatedAt())
                .updatedAt(dto.getUpdatedAt())
                .deletedAt(dto.getDeletedAt())
                .build();
    }
}
