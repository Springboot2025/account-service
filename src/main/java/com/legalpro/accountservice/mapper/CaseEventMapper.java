package com.legalpro.accountservice.mapper;

import com.legalpro.accountservice.dto.CaseEventDto;
import com.legalpro.accountservice.entity.CaseEvent;
import com.legalpro.accountservice.entity.LegalCase;
import com.legalpro.accountservice.repository.LegalCaseRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CaseEventMapper {
    private final LegalCaseRepository legalCaseRepository;

    public CaseEventMapper(LegalCaseRepository legalCaseRepository) {
        this.legalCaseRepository = legalCaseRepository;
    }

    public CaseEventDto toDto(CaseEvent entity) {
        if (entity == null) return null;

        LegalCase legalCase = legalCaseRepository.findByUuid(entity.getCaseUuid())
                .orElseThrow(() -> new RuntimeException("Case not found"));

        return CaseEventDto.builder()
                .id(entity.getId())
                .uuid(entity.getUuid())
                .caseUuid(entity.getCaseUuid())
                .caseId(legalCase.getId())
                .date(entity.getEventDate())
                .type(entity.getEventType())
                .title(entity.getTitle())
                .details(entity.getDetails())
                .status(entity.getStatus())
                .relatedDate(entity.getRelatedDate())
                .userName(entity.getUserName())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public CaseEvent toEntity(CaseEventDto dto) {
        if (dto == null) return null;

        return CaseEvent.builder()
                .id(dto.getId())
                .uuid(UUID.randomUUID())
                .caseUuid(dto.getCaseUuid())
                .eventDate(dto.getDate())
                .eventType(dto.getType())
                .title(dto.getTitle())
                .details(dto.getDetails())
                .status(dto.getStatus())
                .relatedDate(dto.getRelatedDate())
                .userName(dto.getUserName())
                .createdAt(dto.getCreatedAt())
                .build();
    }
}
