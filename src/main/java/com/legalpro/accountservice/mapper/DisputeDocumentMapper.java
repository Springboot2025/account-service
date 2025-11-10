package com.legalpro.accountservice.mapper;

import com.legalpro.accountservice.dto.DisputeDocumentDto;
import com.legalpro.accountservice.entity.DisputeDocument;
import org.springframework.stereotype.Component;

@Component
public class DisputeDocumentMapper {

    public DisputeDocumentDto toDto(DisputeDocument entity) {
        if (entity == null) return null;

        return DisputeDocumentDto.builder()
                .id(entity.getId())
                .disputeUuid(entity.getDisputeUuid())
                .fileName(entity.getFileName())
                .fileType(entity.getFileType())
                .fileUrl(entity.getFileUrl())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
