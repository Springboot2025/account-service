package com.legalpro.accountservice.mapper;

import com.legalpro.accountservice.dto.LegalCaseDto;
import com.legalpro.accountservice.entity.CaseStatus;
import com.legalpro.accountservice.entity.LegalCase;
import org.springframework.stereotype.Component;

@Component
public class LegalCaseMapper {

    public LegalCaseDto toDto(LegalCase entity) {
        if (entity == null) return null;

        return LegalCaseDto.builder()
                .id(entity.getId())
                .uuid(entity.getUuid())
                .caseNumber(entity.getCaseNumber())
                .listing(entity.getListing())
                .courtDate(entity.getCourtDate())
                .availableTrustFunds(entity.getAvailableTrustFunds())
                .followUp(entity.getFollowUp())
                .statusId(entity.getStatus() != null ? entity.getStatus().getId() : null)
                .statusName(entity.getStatus() != null ? entity.getStatus().getName() : null)
                .clientUuid(entity.getClientUuid())
                .lawyerUuid(entity.getLawyerUuid())
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
                .listing(dto.getListing())
                .courtDate(dto.getCourtDate())
                .availableTrustFunds(dto.getAvailableTrustFunds())
                .followUp(dto.getFollowUp())
                .clientUuid(dto.getClientUuid())
                .lawyerUuid(dto.getLawyerUuid())
                .createdAt(dto.getCreatedAt())
                .updatedAt(dto.getUpdatedAt())
                .deletedAt(dto.getDeletedAt());

        // Do not set status here — service should load CaseStatus and call entity.setStatus(caseStatus)
        return builder.build();
    }
}
