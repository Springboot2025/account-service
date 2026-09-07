package com.legalpro.accountservice.mapper;

import com.legalpro.accountservice.dto.DocumentTemplateCenterDto;
import com.legalpro.accountservice.entity.DocumentTemplateCenter;

public class DocumentTemplateCenterMapper {

    private DocumentTemplateCenterMapper() {
        // utility class
    }

    public static DocumentTemplateCenterDto toDto(DocumentTemplateCenter entity) {
        if (entity == null) {
            return null;
        }

        return DocumentTemplateCenterDto.builder()
                .id(entity.getId())
                .uuid(entity.getUuid())
                .subheadingId(entity.getSubheading().getId())
                .fileName(entity.getFileName())
                .fileType(entity.getFileType())
                .fileUrl(com.legalpro.accountservice.util.GcsUrlSigner.sign(entity.getFileUrl()))
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
