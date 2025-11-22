package com.legalpro.accountservice.mapper;

import com.legalpro.accountservice.dto.CourtSupportMaterialDto;
import com.legalpro.accountservice.entity.CourtSupportMaterial;

public class CourtSupportMaterialMapper {

    public static CourtSupportMaterialDto toDto(CourtSupportMaterial e) {
        return CourtSupportMaterialDto.builder()
                .id(e.getId())
                .clientUuid(e.getClientUuid())
                .fileName(e.getFileName())
                .fileType(e.getFileType())
                .fileUrl(e.getFileUrl())
                .caseUuid(e.getCaseUuid())
                .description(e.getDescription())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
