package com.legalpro.accountservice.mapper;

import com.legalpro.accountservice.dto.LetterOfAdviceDocumentDto;
import com.legalpro.accountservice.entity.LetterOfAdviceDocument;
import org.springframework.stereotype.Component;

@Component
public class LetterOfAdviceDocumentMapper {

    public LetterOfAdviceDocumentDto toDto(LetterOfAdviceDocument entity) {
        if (entity == null) return null;

        return LetterOfAdviceDocumentDto.builder()
                .uuid(entity.getUuid())
                .lawyerUuid(entity.getLawyerUuid())
                .clientUuid(entity.getClientUuid())
                .caseUuid(entity.getCaseUuid())
                .templateUuid(entity.getTemplateUuid())
                .title(entity.getTitle())
                .content(entity.getContent())
                .status(entity.getStatus())
                .lawyerSignature(entity.getLawyerSignature())
                .lawyerSignedAt(entity.getLawyerSignedAt())
                .clientSignature(entity.getClientSignature())
                .clientSignedAt(entity.getClientSignedAt())
                .sentToClientAt(entity.getSentToClientAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
