package com.legalpro.accountservice.mapper;

import com.legalpro.accountservice.dto.LegalCaseDto;
import com.legalpro.accountservice.entity.CaseStatus;
import com.legalpro.accountservice.entity.LegalCase;
import com.legalpro.accountservice.entity.Quote;
import com.legalpro.accountservice.repository.QuoteRepository;
import org.springframework.stereotype.Component;

@Component
public class LegalCaseMapper {
    private final QuoteRepository quoteRepository;

    public LegalCaseMapper(QuoteRepository quoteRepository) {
        this.quoteRepository = quoteRepository;
    }

    public LegalCaseDto toDto(LegalCase entity) {
        if (entity == null) return null;

        Quote quote = quoteRepository.findByUuid(entity.getQuoteUuid())
                .orElse(null);

        return LegalCaseDto.builder()
                .id(entity.getId())
                .uuid(entity.getUuid())
                .caseNumber(entity.getCaseNumber())
                .name(entity.getName())
                .listing(entity.getListing())
                .courtDate(entity.getCourtDate())
                .availableTrustFunds(entity.getAvailableTrustFunds())
                .followUp(entity.getFollowUp())
                .statusId(entity.getStatus() != null ? entity.getStatus().getId() : null)
                .statusName(entity.getStatus() != null ? entity.getStatus().getName() : null)
                .caseTypeId(entity.getCaseType() != null ? entity.getCaseType().getId() : null)
                .caseTypeName(entity.getCaseType() != null ? entity.getCaseType().getName() : null)
                .clientUuid(entity.getClientUuid())
                .lawyerUuid(entity.getLawyerUuid())
                .quoteUuid(entity.getQuoteUuid())
                .title(quote != null ? quote.getTitle() : null)
                .offenceList(quote != null ? quote.getOffenceList() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .deletedAt(entity.getDeletedAt())
                .build();
    }

    /**
     * Converts DTO to entity WITHOUT setting CaseStatus.
     * Service layer should resolve and set CaseStatus by id (if provided).
     */
    public LegalCase toEntity(LegalCaseDto dto) {
        if (dto == null) return null;

        LegalCase.LegalCaseBuilder builder = LegalCase.builder()
                .id(dto.getId())
                .uuid(dto.getUuid()) // null is fine; JPA will ignore if id present/absent as needed
                .caseNumber(dto.getCaseNumber())
                .name(dto.getName())
                .listing(dto.getListing())
                .courtDate(dto.getCourtDate())
                .availableTrustFunds(dto.getAvailableTrustFunds())
                .followUp(dto.getFollowUp())
                .clientUuid(dto.getClientUuid())
                .lawyerUuid(dto.getLawyerUuid())
                .quoteUuid(dto.getQuoteUuid())
                .createdAt(dto.getCreatedAt())
                .updatedAt(dto.getUpdatedAt())
                .deletedAt(dto.getDeletedAt());

        // Do not set status here — service should load CaseStatus and call entity.setStatus(caseStatus)
        return builder.build();
    }
}
