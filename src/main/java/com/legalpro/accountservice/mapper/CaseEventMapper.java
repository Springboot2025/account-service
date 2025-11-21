package com.legalpro.accountservice.mapper;

import com.legalpro.accountservice.dto.CaseEventDto;
import com.legalpro.accountservice.entity.CaseEvent;
import org.springframework.stereotype.Component;

@Component
public class CaseEventMapper {

    public CaseEventDto toDto(CaseEvent entity) {
        if (entity == null) return null;

        return CaseEventDto.builder()
                .id(entity.getId())
                .uuid(entity.getUuid())
                .caseUuid(entity.getCaseUuid())
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
                .uuid(dto.getUuid())
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
